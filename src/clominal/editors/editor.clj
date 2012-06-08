(ns clominal.editors.editor
  (:use [clojure.contrib.def])
  (:require [clominal.keys.keymap :as keymap]
            [clominal.action :as action])
  (:import (javax.swing InputMap ActionMap JComponent JTextPane JScrollPane KeyStroke Action SwingUtilities)
           (javax.swing.text DefaultEditorKit JTextComponent TextAction)
           (java.awt.event InputEvent KeyEvent)))

;;------------------------------
;;
;; Maps
;;
;;------------------------------

(def ref-maps (ref {"default" (let [editor (JTextPane.)]
                                [(. editor getInputMap JComponent/WHEN_FOCUSED)
                                 (. editor getActionMap)])}))



;;------------------------------
;;
;; Utilities
;;
;;------------------------------

(defn- create-editor-operation
  "Create operation for editor."
  [src-info]
  (let [default-actionmap ((@ref-maps "default") 1)
        action (cond (string? src-info) (. default-actionmap get src-info)
                     (instance? Action src-info) src-info
                     (fn? src-info) (action/create src-info))]
    (keymap/create-operation ref-maps action)))



;;------------------------------
;;
;; Editor actions
;;
;;------------------------------

;;
;; Caret move action group.
;;

;; Charactor
(defvar forward (create-editor-operation DefaultEditorKit/forwardAction)
  "キャレットを論理的に 1 ポジション順方向に移動する処理の名前です。")
(defvar backward (create-editor-operation DefaultEditorKit/backwardAction)
  "キャレットを論理的に 1 ポジション逆方向に移動する処理の名前です。")

;; Word
(defvar beginWord (create-editor-operation DefaultEditorKit/beginWordAction)
  "キャレットを単語の先頭に移動する処理の名前です。")
(defvar endWord (create-editor-operation DefaultEditorKit/endWordAction)
  "キャレットを単語の末尾に移動する処理の名前です。")
(defvar nextWord (create-editor-operation DefaultEditorKit/nextWordAction)
  "キャレットを次の単語の先頭に移動する処理の名前です。")
(defvar previousWord (create-editor-operation DefaultEditorKit/previousWordAction)
  "キャレットを前の単語の先頭に移動する処理の名前です。")

;; Line
(defvar up (create-editor-operation DefaultEditorKit/upAction)
  "キャレットを論理的に 1 ポジション上に移動する処理の名前です。")
(defvar down (create-editor-operation DefaultEditorKit/downAction)
  "キャレットを論理的に 1 ポジション下に移動する処理の名前です。")
(defvar beginLine (create-editor-operation DefaultEditorKit/beginLineAction)
  "キャレットを行の先頭に移動する処理の名前です。")
(defvar endLine (create-editor-operation DefaultEditorKit/endLineAction)
  "キャレットを行末に移動する処理の名前です。")

;; Paragraph
(defvar beginParagraph (create-editor-operation DefaultEditorKit/beginParagraphAction)
  "キャレットを段落の先頭に移動する処理の名前です。")
(defvar endParagraph (create-editor-operation DefaultEditorKit/endParagraphAction)
  "キャレットを段落の末尾に移動する処理の名前です。")

;; Page
(defvar pageDown (create-editor-operation DefaultEditorKit/pageDownAction)
  "垂直下方にページを切り替える処理の名前です。")
(defvar pageUp (create-editor-operation DefaultEditorKit/pageUpAction)
  "垂直上方にページを切り替える処理の名前です。")

;; Document
(defvar begin (create-editor-operation DefaultEditorKit/beginAction)
  "キャレットをドキュメントの先頭に移動する処理の名前です。")
(defvar end (create-editor-operation DefaultEditorKit/endAction)
  "キャレットをドキュメントの末尾に移動する処理の名前です。")


;;
;; Delete action group.
;;

;; Charactor
(defvar deletePrevChar (create-editor-operation DefaultEditorKit/deletePrevCharAction)
  "現在のキャレットの直前にある 1 文字を削除する処理の名前です。")
(defvar deleteNextChar (create-editor-operation DefaultEditorKit/deleteNextCharAction)
  "現在のキャレットの直後にある 1 文字を削除する処理の名前です。")

;; Word
(defvar deletePrevWord (create-editor-operation DefaultEditorKit/deletePrevWordAction)
  "選択範囲の先頭の前の単語を削除する処理の名前です。")
(defvar deleteNextWord (create-editor-operation DefaultEditorKit/deleteNextWordAction)
  "選択範囲の先頭に続く単語を削除する処理の名前です。")


;;
;; Select group.
;;

(defvar selectWord (create-editor-operation DefaultEditorKit/selectWordAction)
  "キャレットが置かれている単語を選択する処理の名前です。")
(defvar selectLine (create-editor-operation DefaultEditorKit/selectLineAction)
  "キャレットが置かれている行を選択する処理の名前です。")
(defvar selectParagraph (create-editor-operation DefaultEditorKit/selectParagraphAction)
  "キャレットが置かれている段落を選択する処理の名前です。")
(defvar selectAll (create-editor-operation DefaultEditorKit/selectAllAction)
  "ドキュメント全体を選択する処理の名前です。")


;;
;; Move selection group.
;;

;; Selection
(defvar selectionBegin (create-editor-operation DefaultEditorKit/selectionBeginAction)
  "キャレットをドキュメントの先頭に移動する処理の名前です。")
(defvar selectionEnd (create-editor-operation DefaultEditorKit/selectionEndAction)
  "キャレットをドキュメントの末尾に移動する処理の名前です。")

;; Charactor
(defvar selectionForward (create-editor-operation DefaultEditorKit/selectionForwardAction)
  "キャレットを論理的に 1 ポジション順方向に移動して、選択範囲を延ばす処理の名前です。")
(defvar selectionBackward (create-editor-operation DefaultEditorKit/selectionBackwardAction)
  "キャレットを論理的に 1 ポジション逆方向に移動して、選択範囲を延ばす処理の名前です。")

;; Word
(defvar selectionBeginWord (create-editor-operation DefaultEditorKit/selectionBeginWordAction)
  "キャレットを単語の先頭に移動して、選択範囲を延ばす処理の名前です。")
(defvar selectionEndWord (create-editor-operation DefaultEditorKit/selectionEndWordAction)
  "キャレットを単語の末尾に移動して、選択範囲を延ばす処理の名前です。")
(defvar selectionNextWord (create-editor-operation DefaultEditorKit/selectionNextWordAction)
  "選択範囲を次の単語の先頭に移動して、選択範囲を延ばす処理の名前です。")
(defvar selectionPreviousWord (create-editor-operation DefaultEditorKit/selectionPreviousWordAction)
  "選択範囲を前の単語の先頭に移動して、選択範囲を延ばす処理の名前です。")

;; Line
(defvar selectionBeginLine (create-editor-operation DefaultEditorKit/selectionBeginLineAction)
  "キャレットを行の先頭に移動して、選択範囲を延ばす処理の名前です。")
(defvar selectionEndLine (create-editor-operation DefaultEditorKit/selectionEndLineAction)
  "キャレットを行末に移動して、選択範囲を延ばす処理の名前です。")
(defvar selectionUp (create-editor-operation DefaultEditorKit/selectionUpAction)
  "キャレットを論理的に 1 ポジション上方に移動して、選択範囲を延ばす処理の名前です。")
(defvar selectionDown (create-editor-operation DefaultEditorKit/selectionDownAction)
  "キャレットを論理的に 1 ポジション下方に移動して、選択範囲を延ばす処理の名前です。")

;; Paragraph
(defvar selectionBeginParagraph (create-editor-operation DefaultEditorKit/selectionBeginParagraphAction)
  "キャレットを段落の先頭に移動して、選択範囲を延ばす処理の名前です。")
(defvar selectionEndParagraph (create-editor-operation DefaultEditorKit/selectionEndParagraphAction)
  "キャレットを段落の末尾に移動して、選択範囲を延ばす処理の名前です。")


;;
;; Edit operation group.
;;

(defvar copy (create-editor-operation DefaultEditorKit/copyAction)
  "選択された範囲をコピーして、システムのクリップボードに置く処理の名前です。")
(defvar cut (create-editor-operation DefaultEditorKit/cutAction)
  "選択された範囲を切り取り、システムのクリップボードに置く処理の名前です。")
(defvar paste (create-editor-operation DefaultEditorKit/pasteAction)
  "システムのクリップボードの内容を選択された範囲、またはキャレットの前 (選択範囲がない場合) に貼り付ける処理の名前です。")


;;
;; Other group.
;;

(defvar defaultKeyTyped (create-editor-operation DefaultEditorKit/defaultKeyTypedAction)
  "キー入力イベントを受け取ったとき、キーマップエントリがない場合にデフォルトで実行される処理の名前です。")
(defvar insertBreak (create-editor-operation DefaultEditorKit/insertBreakAction)
  "ドキュメントに行/段落の区切りを置く処理の名前です。")
(defvar insertTab (create-editor-operation DefaultEditorKit/insertTabAction)
  "ドキュメントにタブ文字を置く処理の名前です。")
(defvar insertContent (create-editor-operation DefaultEditorKit/insertContentAction)
  "関連するドキュメントに内容を置く処理の名前です。")
(defvar beep (create-editor-operation DefaultEditorKit/beepAction)
  "ビープ音を作成する処理の名前です。")
(defvar readOnly (create-editor-operation DefaultEditorKit/readOnlyAction)
  "エディタを読み込み専用モードに設定する処理の名前です。")
(defvar writable (create-editor-operation DefaultEditorKit/writableAction)
  "エディタを書き込み可能モードに設定する処理の名前です。")
(defvar defaultKeyTyped (create-editor-operation DefaultEditorKit/defaultKeyTypedAction)
  "キー入力イベントを受け取ったとき、キーマップエントリがない場合にデフォルトで実行される処理の名前です。")

;;
;; File action group.
;;

(defvar openFile (create-editor-operation (fn [editor]
                                            (println "called 'openFile'.")))
  "ファイルをオープンします。")
(defvar saveFile (create-editor-operation (fn [editor]
                                            (println "called 'saveFile'.")))
  "ファイルを保存します。")



;;------------------------------
;;
;; Default key bind
;;
;;------------------------------

(defvar- default-settings
  {
   '(Ctrl \g) beginLine
   '(Ctrl \h) backward
   '(Ctrl \j) down
   '(Ctrl \k) up
   '(Ctrl \l) forward
   '(Ctrl \;) endLine

   '(Alt \g) begin
   '(Alt \h) previousWord
   '(Alt \j) pageDown
   '(Alt \k) pageUp
   '(Alt \l) nextWord
   '(Alt \;) end

   '(Ctrl \b) deletePrevChar
   '(Ctrl \d) deleteNextChar
   '((Ctrl \x) (Ctrl \f)) openFile
   '((Ctrl \x) (Ctrl \s)) saveFile
  })

(doseq [setting default-settings]
  (let [key-bind  (setting 0)
        operation (setting 1)]
    (keymap/def-key-bind key-bind operation)))



;;------------------------------
;;
;; Editor
;;
;;------------------------------

(def new-title "新規テキスト")

;; Constractor.
(defn create
  "Create editor pane."
  []
  (let [map-vec           (@ref-maps "default")
        default-inputmap  (map-vec 0)
        default-actionmap (map-vec 1)]
    (def editor (doto (JTextPane.)
                  (.setInputMap  JComponent/WHEN_FOCUSED default-inputmap)
                  (.setActionMap default-actionmap)))
    (JScrollPane. editor JScrollPane/VERTICAL_SCROLLBAR_ALWAYS JScrollPane/HORIZONTAL_SCROLLBAR_ALWAYS)))

