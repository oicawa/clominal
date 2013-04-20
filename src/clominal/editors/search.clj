(ns clominal.editors.search
  (:import (java.awt Font Color Graphics GraphicsEnvironment GridBagLayout Point)
           (java.awt.event ActionEvent)
           (java.awt.im InputMethodRequests)
           (java.beans PropertyChangeListener)
           (java.util HashMap)
           (javax.swing JList InputMap ActionMap JComponent Action
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
           (org.fife.ui.rtextarea RTextScrollPane))
  (:require [clominal.keys :as keys])
  (:require [clominal.editors.editor :as editor])
  (:require [clojure.contrib.string :as string])
  (:use [clominal.utils]))

;;------------------------------
;;
;; Global values
;;
;;------------------------------

;
; Key Maps
;
(def maps (keys/make-keymaps (JTextField.) JComponent/WHEN_FOCUSED))


;;
;; Find
;;
(definterface ISearchPanel
  (setFocus []))

(defn make-search-panel
  [replace?]
  (let [find-caption    (JLabel. "Find:")
        find-textbox    (JTextField.)
        replace-caption (JLabel. "Replace:")
        replace-textbox (JTextField.)
        match-case?     (JCheckBox. "Match Case  " false)
        regular?        (JCheckBox. "Regular Expression  " false)
        filler          (JLabel. "")
        panel           (proxy [JPanel ISearchPanel] []
                          (setFocus []
                            (. find-textbox requestFocusInWindow)))]

    (. replace-caption setVisible replace?)
    (. replace-textbox setVisible replace?)

    (doto panel
      (.setLayout (GridBagLayout.))
      (grid-bag-layout
        :anchor :WEST
        :gridy 0 :gridx 0 :weightx 0.0
        find-caption
        :gridy 0 :gridx 1 :weightx 0.5 :fill :HORIZONTAL
        find-textbox
        :gridy 0 :gridx 2 :weightx 0.0
        match-case?
        :gridy 0 :gridx 3 :weightx 0.0
        regular?
        :gridy 0 :gridx 4 :weightx 0.1
        filler
        :gridy 1 :gridx 0 :weightx 0.0
        replace-caption
        :gridy 1 :gridx 1 :weightx 0.5 :fill :HORIZONTAL
        replace-textbox 
        )
      ;(.setMatchCase true)
      ;(.setRegularExpression false)
      ;(.setSearchFor "let")
      ;(.setSearchForward true)
      )
    ;(. engine find text-pane context)
    ))

(defaction show-find [text-pane]
  (editor/show-sub-panel (make-search-panel false)))

(defaction show-replace [text-pane]
  (editor/show-sub-panel (make-search-panel true)))

