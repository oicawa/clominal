(ns clominal.editors.search
  (:require [clominal.keys :as keys])
  (:require [clominal.editors.editor :as editor])
  (:require [clojure.contrib.string :as string])
  (:use [clominal.utils])
  (:import (java.awt Font Color Graphics GraphicsEnvironment GridBagLayout Point)
           (java.awt.event ActionEvent ActionListener ComponentListener)
           (java.awt.im InputMethodRequests)
           (java.beans PropertyChangeListener)
           (java.util ArrayList)
           (java.util.regex Pattern)
           (javax.swing JList InputMap ActionMap JComponent Action LayoutFocusTraversalPolicy
                        JTextArea
                        JLabel JTextField JPanel JCheckBox JOptionPane SwingConstants JFileChooser
                        SwingUtilities AbstractAction JColorChooser)
           (javax.swing.border LineBorder MatteBorder EmptyBorder CompoundBorder)
           (javax.swing.event CaretListener DocumentListener)
           (javax.swing.text StyleConstants Utilities DefaultEditorKit DefaultHighlighter$DefaultHighlightPainter SimpleAttributeSet
                             DefaultStyledDocument StyleContext)
           (javax.swing.undo UndoManager)
           (java.io File FileInputStream FileWriter FileNotFoundException StringReader)
           (clojure.lang LineNumberingPushbackReader LispReader Reflector)
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
  (getSearchContext [])
  (getTextPane [])
  (isReplace [])
  (setReplace [replace])
  (isSelectionOnly [])
  (setSelectionOnly [selection?])
  (getSelectionStart [])
  (getSelectionEnd [])
  (getHighlightTags []))
  

(definterface IInputField
  (getUndoManager []))


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
   match-case-check
   regular-check
   filler
   root           
   policy         
   default-map]
  (proxy [JTextField IInputField IMarkable IKeybindComponent] []
    ; (transferFocusBackward []
    ;   (. @replace-textbox requestFocus))
    (getUndoManager [] um)
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
        






;	String text = context.getSearchFor();
;	if (text==null || text.length()==0) {
;		return false;
;	}
;	// Be smart about what position we're "starting" at.  We don't want
;	// to find a match in the currently selected text (if any), so we
;	// start searching AFTER the selection if searching forward, and
;	// BEFORE the selection if searching backward.
;	Caret c = textArea.getCaret();
;	boolean forward = context.getSearchForward();
;	int start = forward ? Math.max(c.getDot(), c.getMark()) :
;					Math.min(c.getDot(), c.getMark());
;	String findIn = getFindInText(textArea, start, forward);
;	if (findIn==null || findIn.length()==0) return false;
;
;	// Find the next location of the text we're searching for.
;	if (!context.isRegularExpression()) {
;		int pos = getNextMatchPos(text, findIn, forward,
;							context.getMatchCase(), context.getWholeWord());
;		findIn = null; // May help garbage collecting.
;		if (pos!=-1) {
;			// Without this, if JTextArea isn't in focus, selection
;			// won't appear selected.
;			c.setSelectionVisible(true);
;			pos = forward ? start+pos : pos;
;			selectAndPossiblyCenter(textArea, pos, pos+text.length());
;			return true;
;		}
;	}
;	else {
;		// Regex matches can have varying widths.  The returned point's
;               // x- and y-values represent the start and end indices of the
;	        // match in findIn.
;	        Point regExPos = getNextMatchPosRegEx(text, findIn, forward,
;						context.getMatchCase(), context.getWholeWord());
;	        findIn = null; // May help garbage collecting.
;	        if (regExPos!=null) {
;		   // Without this, if JTextArea isn't in focus, selection
;		   // won't appear selected.
;		   c.setSelectionVisible(true);
;		   if (forward) {
;			regExPos.translate(start, start);
;		   }
;		selectAndPossiblyCenter(textArea, regExPos.x, regExPos.y);
;		return true;
;	}
;}
;
;// No match.
;return false;


;	private static void  [More ...] selectAndPossiblyCenter(JTextArea textArea, int start,
;												int end) {
;		boolean foldsExpanded = false;
;		if (textArea instanceof RSyntaxTextArea) {
;			RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;
;			FoldManager fm = rsta.getFoldManager();
;			if (fm.isCodeFoldingSupportedAndEnabled()) {
;				foldsExpanded = fm.ensureOffsetNotInClosedFold(start);
;				foldsExpanded |= fm.ensureOffsetNotInClosedFold(end);
;			}
;		}
;		textArea.setSelectionStart(start);
;		textArea.setSelectionEnd(end);
;		Rectangle r = null;
;		try {
;			r = textArea.modelToView(start);
;			if (r==null) { // Not yet visible; i.e. JUnit tests
;				return;
;			}
;			if (end!=start) {
;				r = r.union(textArea.modelToView(end));
;			}
;		} catch (BadLocationException ble) { // Never happens
;			ble.printStackTrace();
;			textArea.setSelectionStart(start);
;			textArea.setSelectionEnd(end);
;			return;
;		}
;		Rectangle visible = textArea.getVisibleRect();
;		// If the new selection is already in the view, don't scroll,
;		// as that is visually jarring.
;		if (!foldsExpanded && visible.contains(r)) {
;			textArea.setSelectionStart(start);
;			textArea.setSelectionEnd(end);
;			return;
;		}
;		visible.x = r.x - (visible.width - r.width) / 2;
;		visible.y = r.y - (visible.height - r.height) / 2;
;		Rectangle bounds = textArea.getBounds();
;		Insets i = textArea.getInsets();
;		bounds.x = i.left;
;		bounds.y = i.top;
;		bounds.width -= i.left + i.right;
;		bounds.height -= i.top + i.bottom;
;		if (visible.x < bounds.x) {
;			visible.x = bounds.x;
;		}
;		if (visible.x + visible.width > bounds.x + bounds.width) {
;			visible.x = bounds.x + bounds.width - visible.width;
;		}
;
;		if (visible.y < bounds.y) {
;			visible.y = bounds.y;
;		}
;
;		if (visible.y + visible.height > bounds.y + bounds.height) {
;			visible.y = bounds.y + bounds.height - visible.height;
;		}
;		textArea.scrollRectToVisible(visible);
;	}
;

(defn getFindInText
  [text-pane start end forward?]
  (let [length (- end start)]
    (. text-pane getText start length)))


(defn is-fold?
  [text-pane pos]
  (let [fm (. text-pane getFoldManager)]
    (if (. fm isCodeFoldingSupportedAndEnabled)
        (. fm ensureOffsetNotInClosedFold pos)
        false)))

(defn selectAndPossiblyCenter
  [text-pane start end] ; int int
  (let [foldsExpanded (or (is-fold? text-pane start) (is-fold? text-pane end))]
    (. text-pane setSelectionStart start)
    (. text-pane setSelectionEnd end)
    (let [rect1 (. text-pane modelToView start)
          rect2 (cond (nil? rect1)
                        nil
                      (not (= start end))
                        (. rect1 union (. text-pane modelToView end))
                      :else
                        rect1)]
      (if (nil? rect2)
          nil
          (let [visible (. text-pane getVisibleRect)
                inset   (. text-pane getInsets)
                bounds  (. text-pane getBounds)]
            (if (and (not foldsExpanded) (. visible contains rect2))
                (do
                  (. text-pane setSelectionStart start)
                  (. text-pane setSelectionEnd end)
                  nil)
                (do
                  (. visible x (- (. rect2 x) (/ (- (. visible width) (. rect2 width)) 2)))
                  (. visible y (- (. rect2 y) (/ (- (. visible height) (. rect2 height)) 2)))
                  (. bounds x (. inset left))
                  (. bounds y (. inset top))
                  (. bounds width (- (. bounds width) (. inset left) (. inset right)))
                  (. bounds height (- (. bounds height) (. inset top) (. inset bottom)))
                  (if (< (. visible x) (. bounds x))
                      (. visible x (. bounds x)))
                  (if (> (+ (. visible x) (. visible width)) (+ (. bounds x) (. bounds width)))
                      (. visible x (- (+ (. bounds x) (. bounds width)) (. visible width))))
                  (if (< (. visible y) (. bounds y))
                      (. visible y (. bounds y)))
                  (if (> (+ (. visible y) (. visible height)) (+ (. bounds y) (. bounds height)))
                      (. visible y (- (+ (. bounds y) (. bounds height)) (. visible height))))
                  (. text-pane scrollRectToVisible visible))))))))

(defn get-find-regex-positions
  [text-pane start end baseRegEx matchCase wholeWord]
  (let [searchIn (. text-pane getText start (- end start))
        regEx    (if wholeWord (str "\\b" baseRegEx "\\b") baseRegEx)
        flags    (+ Pattern/MULTILINE     ;; '^' and '$' are done per line.
                    (if matchCase 0 (+ Pattern/CASE_INSENSITIVE Pattern/UNICODE_CASE)))
        pattern  (Pattern/compile regEx flags)
        matcher  (. pattern matcher searchIn)]
    (loop [result ()]
      (if (. matcher find)
          (recur (cons (Point. (+ start (. matcher start)) (+ start (. matcher end))) result))
          (reverse result)))))
          
(defn get-find-normal-positions
  [text-pane start end search-for match-case? whole-word?]
  (loop [offset start
         result '()]
    (if (< end offset)
        (reverse result)
        (let [length        (+ (- end offset) 1)
              search-in     (. text-pane getText offset length)
              search-in-pos (SearchEngine/getNextMatchPos search-for search-in true match-case? whole-word?)]
          (if (< search-in-pos 0)
              (reverse result)
              (let [total-start-pos   (+ offset search-in-pos)
                    total-end-pos     (+ total-start-pos (count search-for))
                    next-offset (+ total-start-pos 1)]
                (recur next-offset (cons (Point. total-start-pos total-end-pos) result))))))))

(defn get-find-positions
  [search-panel & term]
  (assert (or (nil? term) (= (count term) 2)))
  (let [context     (. search-panel getSearchContext)
        search-for  (. context getSearchFor)
        forward?    true
        match-case? (. context getMatchCase)
        whole-word? (. context getWholeWord)
        regular?    (. context isRegularExpression)
        selection?  (. search-panel isSelectionOnly)
        text-pane   (. search-panel getTextPane)
        start       (if (nil? term)
                        (if selection? (. text-pane getSelectionStart) 0)
                        ((vec term) 0))
        end         (if (nil? term)
                        (if selection? (. text-pane getSelectionEnd) (.. text-pane getDocument getLength))
                        ((vec term) 1))]
    (if regular?
        (get-find-regex-positions text-pane start end search-for match-case? whole-word?)
        (get-find-normal-positions text-pane start end search-for match-case? whole-word?))))

(def matched-painter (DefaultHighlighter$DefaultHighlightPainter. Color/GRAY))

(defn move-sbling-highlight-pos
  [search-panel forward?]
  (let [text-pane   (. search-panel getTextPane)
        highlighter (. text-pane getHighlighter)
        highlights  (filter #(= matched-painter (. %1 getPainter))
                            (. highlighter getHighlights))
        positions   (let [tmp-positions (map #(. %1 getStartOffset) highlights)]
                      (if forward? tmp-positions (reverse tmp-positions)))
        caret-pos   (. text-pane getCaretPosition)
        judge       (if forward? < >)
        next-pos    (first (filter #(judge caret-pos %1) positions))]
    (if (not (nil? next-pos))
        (. text-pane setCaretPosition next-pos))))

(defaction find-operate
  [find-textbox]
  (let [search-panel   (. find-textbox getParent)
        context        (. search-panel getSearchContext)
        highlight-tags (. search-panel getHighlightTags)
        text-pane      (. search-panel getTextPane)
        highlighter    (. text-pane getHighlighter)
        search-for     (. context getSearchFor)
        positions      (get-find-positions search-panel)]
    (doseq [highlight-tag highlight-tags]
      (. highlighter removeHighlight highlight-tag))
    (. highlight-tags clear)
    (doseq [pos positions]
      (let [new-tag (. highlighter addHighlight (. pos getX) (. pos getY) matched-painter)]
        (. highlight-tags add new-tag)))
    (move-sbling-highlight-pos search-panel true)))

(defaction hide [find-textbox]
  (println "Hide search-panel.")
  (if (. find-textbox isMark)
      (. find-textbox setMark false)
      (let [search-panel (. find-textbox getParent)
            sub-panel    (. search-panel getParent)
            root-panel   (. sub-panel getParent)]
        (. search-panel setVisible false)
        (. root-panel setFocus))))

(declare print-highlights)
(declare refresh-highlights)
(defn make-search-panel
  [text-pane]
  (let [improved-imr     (atom nil)
        ime-mode         (atom nil)
        find-um          (UndoManager.)
        replace-um       (UndoManager.)
        is-marked        (atom false)
        find-caption     (atom nil)
        find-textbox     (atom nil)
        replace-caption  (atom nil)
        replace-textbox  (atom nil)
        match-case-check (atom nil)
        regular-check    (atom nil)
        selection-check  (atom nil)
        filler           (atom nil)
        root             (atom nil)
        policy           (atom nil)
        default-map      (. editor/single-line-maps get "default")
        context          (SearchContext.)
        selection-start  (. text-pane getSelectionStart)
        selection-end    (. text-pane getSelectionEnd)
        highlight-tags   (ArrayList.)
        ]

    (println "==========")
    (println "Selection start :" selection-start)
    (println "          end   :" selection-end)

    (doto (. text-pane getDocument)
      (.addDocumentListener
        (proxy [DocumentListener] []
          (changedUpdate [evt])
          (insertUpdate [evt]
            (let [start-line   (editor/get-line-element text-pane (editor/get-line-index text-pane (. evt getOffset)))
                  end-line     (editor/get-line-element text-pane (editor/get-line-index text-pane (+ (. evt getOffset) (. evt getLength))))
                  region-start (. start-line getStartOffset)
                  region-end   (. end-line getEndOffset)]
              (refresh-highlights text-pane region-start region-end)))
          (removeUpdate [evt]
            (let [start-line   (editor/get-line-element text-pane (editor/get-line-index text-pane (. evt getOffset)))
                  end-line     (editor/get-line-element text-pane (editor/get-line-index text-pane (+ (. evt getOffset) (. evt getLength))))
                  region-start (. start-line getStartOffset)
                  region-end   (. end-line getEndOffset)]
              (refresh-highlights text-pane region-start region-end))))))

    (reset! find-caption
      (JLabel. "Find:"))

    (reset! find-textbox
      (doto (make-textbox text-pane improved-imr ime-mode find-um is-marked
                          find-caption find-textbox
                          replace-caption replace-textbox
                          match-case-check regular-check
                          filler root policy default-map)
        (.setFocusAccelerator \F)
        (.setInputMap  JComponent/WHEN_FOCUSED (. default-map getInputMap))
        (.setActionMap (. default-map getActionMap))))

    (doto (. @find-textbox getDocument)
      (.addUndoableEditListener find-um)
      (.addDocumentListener
        (proxy [DocumentListener] []
          (changedUpdate [evt])
          (insertUpdate [evt] (. context setSearchFor (. @find-textbox getText)))
          (removeUpdate [evt] (. context setSearchFor (. @find-textbox getText))))))

    (reset! replace-caption (JLabel. "Replace:"))

    (reset! replace-textbox
      (doto (make-textbox text-pane improved-imr ime-mode replace-um is-marked
                          find-caption find-textbox
                          replace-caption replace-textbox
                          match-case-check regular-check
                          filler root policy default-map)
        (.setFocusAccelerator \R)
        (.setInputMap  JComponent/WHEN_FOCUSED (. default-map getInputMap))
        (.setActionMap (. default-map getActionMap))))

    (doto (. @replace-textbox getDocument)
      (.addUndoableEditListener replace-um)
      (.addDocumentListener
        (proxy [DocumentListener] []
          (changedUpdate [evt])
          (insertUpdate [evt] (. context setReplaceWith (. @replace-textbox getText)))
          (removeUpdate [evt] (. context setReplaceWith (. @replace-textbox getText))))))

    (reset! match-case-check
      (doto (JCheckBox. "Match Case  " false)
        (.setMnemonic (keys/normal-keys 'm))
        (.addActionListener
          (proxy [ActionListener] []
            (actionPerformed [evt]
              (println "Match Case:" (. @match-case-check isSelected))
              (. context setMatchCase (. @match-case-check isSelected)))))))

    (reset! regular-check
      (doto (JCheckBox. "Regular Expression  " false)
        (.setMnemonic (keys/normal-keys 'e))
        (.addActionListener
          (proxy [ActionListener] []
            (actionPerformed [evt]
              (println "Regular Expression:" (. @regular-check isSelected))
              (. context setRegularExpression (. @regular-check isSelected)))))))

    (reset! selection-check
      (doto (JCheckBox. "In selection  " (< selection-start selection-end))
        (.setMnemonic (keys/normal-keys 's))
        (.addActionListener
          (proxy [ActionListener] []
            (actionPerformed [evt]
              (println "In Selection:" (. @selection-check isSelected))
              (. context setSearchSelectionOnly (. @selection-check isSelected)))))))

    (reset! filler
      (JLabel. ""))

    (reset! root
      (doto (proxy [JPanel ISearchPanel] []
              (setFocus []
                (. @find-textbox requestFocusInWindow))
              (getSearchContext [] context)
              (getTextPane []
                text-pane)
              (isReplace []
                (. @replace-textbox isVisible))
              (setReplace [replace]
                (. @replace-caption setVisible replace)
                (. @replace-textbox setVisible replace))
              (getSelectionStart []
                (. text-pane getSelectionStart))
              (getSelectionEnd []
                (. text-pane getSelectionEnd))
              (isSelectionOnly []
                (. @selection-check isSelected))
              (setSelectionOnly [selection]
                (. @selection-check setSelected selection))
              (getHighlightTags []
                highlight-tags))
        (.setLayout (GridBagLayout.))
        (.addComponentListener (proxy [ComponentListener] []
                                 (componentHidden [evt])
                                 (componentMoved [evt])
                                 (componentResized [evt])
                                 (componentShown [evt]
                                   (let [start      (. text-pane getSelectionStart)
                                         end        (. text-pane getSelectionEnd)
                                         selection? (< start end)]
                                     (println (format "start: %d, end: %d, selection?:" start end) selection?)
                                     (doto @selection-check 
                                       (.setEnabled selection?)
                                       (.setSelected selection?))))))
        (grid-bag-layout
          :anchor :WEST
          :gridy 0 :gridx 0 :weightx 0.0
          @find-caption
          :gridy 0 :gridx 1 :weightx 1.0 :fill :HORIZONTAL
          @find-textbox
          :gridy 0 :gridx 2 :weightx 0.0
          @match-case-check
          :gridy 0 :gridx 3 :weightx 0.0
          @regular-check
          :gridy 0 :gridx 4 :weightx 0.0
          @selection-check
          :gridy 0 :gridx 5 :weightx 0.0
          @filler
          :gridy 1 :gridx 0 :weightx 0.0
          @replace-caption
          :gridy 1 :gridx 1 :weightx 1.0 :fill :HORIZONTAL
          @replace-textbox)
        ; (.setFocusTraversalPolicy @policy)
        ))
    @root))

(defn get-search-panel
  [text-pane]
  (let [root-panel   (. text-pane getRoot)
        search-panel (. root-panel getSubPanel "clominal.editors.search")]
    (if (nil? search-panel)
        (let [tmp-panel (make-search-panel text-pane)]
          (. root-panel addSubPanel "clominal.editors.search" tmp-panel)
          tmp-panel)
        search-panel)))

(defn print-highlights
  [text-pane]
  (let [search-panel   (get-search-panel text-pane)
        highlight-tags (. search-panel getHighlightTags)]
    (doseq [highlight-tag highlight-tags]
      (let [start (. highlight-tag getStartOffset)
            end   (. highlight-tag getEndOffset)
            value (. text-pane getText start (- end start))]
       (println (format "start: %d, end: %d, [%s]" start end value))))))

(defn refresh-highlights
  [text-pane region-start region-end]
  (let [highlighter    (. text-pane getHighlighter)
        search-panel   (get-search-panel text-pane)
        highlight-tags (. search-panel getHighlightTags)
        remove-targets (filter #(let [highlight-tag %1
                                      start         (. highlight-tag getStartOffset)
                                      end           (. highlight-tag getEndOffset)]
                                  (or (<= region-start start region-end)
                                      (<= region-start end   region-end)))
                               highlight-tags)]

    ; remove highlights in region
    (doseq [highlight-tag remove-targets]
      (. highlighter removeHighlight highlight-tag))
    (doseq [highlight-tag remove-targets]
      (. highlight-tags remove highlight-tag))

    ; add new highlights
    (doseq [pos (get-find-positions search-panel region-start region-end)]
      (let [new-tag (. highlighter addHighlight (. pos getX) (. pos getY) matched-painter)]
        (. highlight-tags add new-tag)))))

(defaction show-as-find [text-pane]
  (let [root-panel   (. text-pane getRoot)
        search-panel (get-search-panel text-pane)]
    (. search-panel setReplace false)
    (. search-panel setVisible true)
    (. search-panel setFocus)))

(defaction find-next-in-search-panel [textbox]
  (let [search-panel (. textbox getParent)]
    (move-sbling-highlight-pos search-panel true)))

(defaction find-prev-in-search-panel [textbox]
  (let [search-panel (. textbox getParent)]
    (move-sbling-highlight-pos search-panel false)))

(defaction find-next [text-pane]
  (let [search-panel (get-search-panel text-pane)]
    (move-sbling-highlight-pos search-panel true)))

(defaction find-prev [text-pane]
  (let [search-panel (get-search-panel text-pane)]
    (move-sbling-highlight-pos search-panel false)))

(defaction show-as-replace [text-pane]
  (let [root-panel   (. text-pane getRoot)
        search-panel (get-search-panel text-pane)]
    (. search-panel setReplace true)
    (. search-panel setVisible true)
    (. search-panel setFocus)))



