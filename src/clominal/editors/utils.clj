(ns clominal.editors.utils
  (:use [clojure.contrib.def])
  (:require [clominal.keys.keymap :as keymap]
            [clominal.action :as action]
            [clominal.utils.guiutils :as guiutils]
            [clominal.utils.env :as env])
  (:import (java.awt Font GraphicsEnvironment GridBagLayout)
           (javax.swing InputMap ActionMap JComponent JTextPane JScrollPane Action JLabel JTextField JPanel)
           (javax.swing.text DefaultEditorKit)
           (clominal.editors MiddleKeyAction LastKeyAction)
           ))

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

(defn create-editor-operation
  "Create operation for editor."
  [src-info]
  (let [default-actionmap ((@ref-maps "default") 1)
        action (cond (string? src-info) (. default-actionmap get src-info)
                     (instance? Action src-info) src-info
                     (fn? src-info) (action/create src-info))]
    (keymap/create-operation ref-maps action)))


(defn def-key-bind
  "Define key bind for specified operation for one stroke or more, for EditorPanel."
  [key-bind operation]
  (let [ref-maps          (operation :ref-maps)
        action            (operation :action)
        default-maps      (@ref-maps "default")
        default-inputmap  (default-maps 0)
        default-actionmap (default-maps 1)
        all-strokes       (keymap/get-key-strokes key-bind)]
    (if (seq? all-strokes)
        (loop [inputmap    default-inputmap
               actionmap   default-actionmap
               stroke      (first all-strokes)
               strokes     (rest all-strokes)]
          (let [stroke-name (str stroke)]
            (if (= nil (first strokes))
                (do
                  (. inputmap  put stroke stroke-name)
                  (. actionmap put stroke-name (LastKeyAction. action default-inputmap default-actionmap)))
                (let [map-vec        (keymap/get-maps ref-maps stroke-name)
                      next-inputmap  (map-vec 0)
                      next-actionmap (map-vec 1)
                      ;middle-action  nil
                      middle-action  (MiddleKeyAction. stroke next-inputmap next-actionmap)
                      ]
                  (. inputmap  put stroke stroke-name)
                  (. actionmap put stroke-name middle-action)
                  (recur next-inputmap next-actionmap (first strokes) (rest strokes))))))
        (do
          (. default-inputmap  put all-strokes (str all-strokes))
          (. default-actionmap put (str all-strokes) action)
          ;(print-maps (str all-strokes) default-inputmap default-actionmap)
          ))))

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
(defvar deletenextword (create-editor-operation DefaultEditorKit/deleteNextWordAction)
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

;;
;; File action group.
;;

(defvar openFile
  (create-editor-operation
    (fn [editor]
      (println "called 'openFile'.")
      (let [tab (.. editor getParent getParent getParent getParent)
            components (. tab getComponents)]
        (doseq [component components]
          (println (. component getName))))
      ))
  "ファイルをオープンします。")

(defvar saveFile
  (create-editor-operation
    (fn [editor]
      (println "called 'saveFile'.")
      (println "----------")
      (println (.. editor getText))))
  "ファイルを保存します。")

(defvar changeBuffer
  (create-editor-operation
    (fn [editor]
      (println "called 'changeBuffer'.")))
  "表示するバッファを変更します。")
          


;;------------------------------
;;
;; Default key bind
;;
;;------------------------------

(defvar- default-settings
  {
   '(Ctrl \h) beginLine
   '(Ctrl \j) backward
   '(Ctrl \k) down
   '(Ctrl \l) up
   '(Ctrl \;) forward
   '(Ctrl \:) endLine

   '(Alt \h) begin
   '(Alt \j) previousWord
   '(Alt \k) pageDown
   '(Alt \l) pageUp
   '(Alt \;) nextWord
   '(Alt \:) end

   '(Ctrl \b) deletePrevChar
   '(Ctrl \d) deleteNextChar
   '((Ctrl \x) (Ctrl \f)) openFile
   '((Ctrl \x) (Ctrl \s)) saveFile
   '((Ctrl \x) \b) changeBuffer
   '((Ctrl \x) (Alt \a) \s) selectAll
   '((Ctrl Alt \a) \s) selectAll
  })

(doseq [setting default-settings]
  (let [key-bind  (setting 0)
        operation (setting 1)]
    ;(keymap/def-key-bind key-bind operation)
    (def-key-bind key-bind operation)))



;;------------------------------
;;
;; Editor
;;
;;------------------------------

(def new-title "新規テキスト")

(defn get-font-names
  []
  (doseq [font (.. GraphicsEnvironment getLocalGraphicsEnvironment getAvailableFontFamilyNames)]
    (println font)))

(defn set-font
  [component name type size]
  (. component setFont (Font. name type size)))


