(ns clominal.dialog
  (:import (java.awt Font Color GraphicsEnvironment GridBagLayout)
           (java.awt.event ActionEvent)
           (java.awt.im InputMethodRequests)
           (javax.swing JDialog InputMap ActionMap JComponent JTextPane JScrollPane Action JLabel JTextField JPanel JOptionPane 
                        WindowConstants SwingConstants)
           (javax.swing.border LineBorder)
           (java.io File FileInputStream FileWriter FileNotFoundException))
  (:require [clominal.utils :as utils]))

(defn make-dialog
  [title panel]
  (doto (JDialog.)
    (.setTitle title)
    (.setModal true)
    (.setDefaultCloseOperation WindowConstants/DISPOSE_ON_CLOSE)
    (.setSize 400 300)
    (.add panel)
    (.setLocationRelativeTo nil)))


                
