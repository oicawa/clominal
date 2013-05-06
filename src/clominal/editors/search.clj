(ns clominal.editors.search
  (:require [clominal.keys :as keys])
  (:require [clominal.editors.editor :as editor])
  (:require [clojure.contrib.string :as string])
  (:use [clominal.utils])
  (:import (java.awt Font Color Graphics GraphicsEnvironment GridBagLayout Point)
           (java.awt.event ActionEvent)
           (java.awt.im InputMethodRequests)
           (java.beans PropertyChangeListener)
           (java.util HashMap)
           (javax.swing JList InputMap ActionMap JComponent Action LayoutFocusTraversalPolicy
                        ; JTextPane JScrollPane
                        JLabel JTextField JPanel JCheckBox JOptionPane SwingConstants JFileChooser
                        SwingUtilities AbstractAction JColorChooser)
           (javax.swing.border LineBorder MatteBorder EmptyBorder CompoundBorder)
           (javax.swing.event CaretListener DocumentListener)
           (javax.swing.text StyleConstants Utilities DefaultEditorKit DefaultHighlighter$DefaultHighlightPainter SimpleAttributeSet
                             DefaultStyledDocument StyleContext)
           (javax.swing.undo UndoManager)
           (java.io File FileInputStream FileWriter FileNotFoundException StringReader)
           (clojure.lang LineNumberingPushbackReader LispReader)
           (org.fife.ui.rsyntaxtextarea RSyntaxTextArea SyntaxConstants TextEditorPane FileLocation Token RSyntaxUtilities)
           (org.fife.ui.rtextarea RTextScrollPane SearchContext SearchEngine)
           (clominal.keys IKeybindComponent)
           (clominal.utils IMarkable)))

;;------------------------------
;;
;; Global values
;;
;;------------------------------

;;
;; Find
;;
(definterface ISearchPanel
  (setFocus [])
  (getSearchContext [forward?])
  (getTextPane [])
  (isReplace [])
  (isAll []))

(defn make-textbox
  [text-pane
   improved-imr
   ime-mode
   um
   is-marked
   find-caption
   find-textbox
   replace-caption
   replace-textbox
   match-case?    
   regular?       
   filler         
   root           
   policy         
   default-map]
  (proxy [JTextField IMarkable IKeybindComponent] []
    ; (transferFocusBackward []
    ;   (. @replace-textbox requestFocus))
    (getKeyMaps [] editor/single-line-maps)
    (getInputMethodRequests []
      (if (= nil @improved-imr)
          (let [original-imr (proxy-super getInputMethodRequests)]
            (reset! improved-imr (editor/make-improved-imr original-imr))
            @improved-imr)
          @improved-imr))
    (setImeEnable [value]
      (if (windows?)
          (let [ic (. this getInputContext)]
            (if value
                (do
                  (. ic setCompositionEnabled (if (= nil @ime-mode) (. ic isCompositionEnabled) @ime-mode))
                  (reset! ime-mode nil))
                (do
                  (reset! ime-mode (. ic isCompositionEnabled))
                  (. ic setCompositionEnabled false))))
          (. this setEditable value)))
    (setInputMap [inputmap] (. this setInputMap JComponent/WHEN_FOCUSED inputmap))
    (setActionMap [actionmap] (proxy-super setActionMap actionmap))
    (setKeyStroke [keystroke]
      ; (if (= nil keystroke)
      ;     (. keystrokes setText "")
      ;     (let [current (. keystrokes getText)]
      ;       (. keystrokes setText (if (= current "")
      ;                                 (keys/str-keystroke keystroke)
      ;                                 (str current ", " (keys/str-keystroke keystroke))))))
                                      )
    (isMark [] @is-marked)
    (setMark [marked]
      (reset! is-marked marked))))


 []
(defn make-search-panel
  [text-pane replace?]
  (let [improved-imr    (atom nil)
        ime-mode        (atom nil)
        um              (UndoManager.)
        is-marked       (atom false)
        find-caption    (atom nil)
        find-textbox    (atom nil)
        replace-caption (atom nil)
        replace-textbox (atom nil)
        match-case?     (atom nil)
        regular?        (atom nil)
        is-all?         (atom nil)
        filler          (atom nil)
        root            (atom nil)
        policy          (atom nil)
        default-map     (. editor/single-line-maps get "default")]
    (reset! find-caption
      (JLabel. "Find:"))

    (reset! find-textbox
      (doto (make-textbox text-pane improved-imr ime-mode um is-marked
                          find-caption find-textbox
                          replace-caption replace-textbox
                          match-case? regular?
                          filler root policy default-map)
        (.setFocusAccelerator \F)
        (.setInputMap  JComponent/WHEN_FOCUSED (. default-map getInputMap))
        (.setActionMap (. default-map getActionMap))))

    (reset! replace-caption
      (doto (JLabel. "Replace:")
        (.setVisible replace?)))

    (reset! replace-textbox
      (doto (make-textbox text-pane improved-imr ime-mode um is-marked
                          find-caption find-textbox
                          replace-caption replace-textbox
                          match-case? regular?
                          filler root policy default-map)
        (.setFocusAccelerator \R)
        (.setInputMap  JComponent/WHEN_FOCUSED (. default-map getInputMap))
        (.setActionMap (. default-map getActionMap))
        (.setVisible replace?)))

    (reset! match-case?
      (doto (JCheckBox. "Match Case  " false)
        (.setMnemonic (keys/normal-keys 'm))))

    (reset! regular?
      (doto ; (proxy [JCheckBox] ["Regular Expression  " false]
            ;   (transferFocus []
            ;     (. @find-textbox requestFocus)))
            (JCheckBox. "Regular Expression  " false)
        (.setMnemonic (keys/normal-keys 'e))))

    (reset! is-all?
      (doto (JCheckBox. "All  " false)
        (.setMnemonic (keys/normal-keys 'a))))

    (reset! filler
      (JLabel. ""))

    (reset! policy (proxy [LayoutFocusTraversalPolicy] []
                     (getComponentAfter [container component]
                       (println "getComponentAfter")
                       (cond (= component @find-textbox)
                              (if replace? @replace-textbox @match-case?)
                             (= component @replace-textbox)
                              @match-case?
                             (= component @match-case?)
                              @regular?
                             (= component @regular?)
                              @find-textbox
                             :else
                              (assert false)))
                     (getComponentBefore [container component]
                       (println "getComponentBefore")
                       (cond (= component @find-textbox)
                              @regular?
                             (= component @replace-textbox)
                              @find-textbox
                             (= component @match-case?)
                              (if replace? @replace-textbox @find-textbox)
                             (= component @regular?)
                              @match-case?
                             :else
                              (assert false)))))

    (reset! root
      (doto (proxy [JPanel ISearchPanel] []
              (setFocus []
                (. @find-textbox requestFocusInWindow))
              (getSearchContext [forward?]
                (let [context (SearchContext.)]
                  (doto context
                    (.setSearchForward forward?)
                    (.setMatchCase (. @match-case? isSelected))
                    (.setRegularExpression (. @regular? isSelected))
                    (.setSearchFor (. @find-textbox getText))
                    (.setReplaceWith (. @replace-textbox getText)))))
              (getTextPane []
                text-pane)
              (isReplace []
                (. @replace-textbox isVisible))
              (isAll []
                (. @is-all? isSelected)))
        (.setLayout (GridBagLayout.))
        (grid-bag-layout
          :anchor :WEST
          :gridy 0 :gridx 0 :weightx 0.0
          @find-caption
          :gridy 0 :gridx 1 :weightx 1.0 :fill :HORIZONTAL
          @find-textbox
          :gridy 0 :gridx 2 :weightx 0.0
          @match-case?
          :gridy 0 :gridx 3 :weightx 0.0
          @regular?
          :gridy 0 :gridx 4 :weightx 0.0
          @is-all?
          :gridy 0 :gridx 5 :weightx 0.0
          @filler
          :gridy 1 :gridx 0 :weightx 0.0
          @replace-caption
          :gridy 1 :gridx 1 :weightx 1.0 :fill :HORIZONTAL
          @replace-textbox)
        (.setFocusTraversalPolicy @policy)))
    @root))

(defn operate [textbox forward?]
  (let [root      (. textbox getParent)
        context   (. root getSearchContext forward?)
        text-pane (. root getTextPane)]
    (if (. root isReplace)
        (if (. root isAll)
            (println "replace:" (SearchEngine/replace text-pane context))
            (println "replace all:" (SearchEngine/replaceAll text-pane context)))
        (if (. root isAll)
            (println "*** NOT IMPLEMENTED ***")
            (println "find:" (SearchEngine/find text-pane context))))))

(defaction operate-forward [textbox]
  (operate textbox true))

(defaction operate-backword [textbox]
  (operate textbox false))

(defaction escape [find-textbox]
  (println "Close search-panel.")
  (if (. find-textbox isMark)
      (. find-textbox setMark false)
      (let [search-panel (. find-textbox getParent)
            sub-panel    (. search-panel getParent)
            root-panel   (. sub-panel getParent)]
        (. sub-panel remove search-panel)
        (. root-panel setFocus))))

(defaction show-find [text-pane]
  (editor/show-sub-panel text-pane (make-search-panel text-pane false)))

(defaction show-replace [text-pane]
  (editor/show-sub-panel text-pane (make-search-panel text-pane true)))

