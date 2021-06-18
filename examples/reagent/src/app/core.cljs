(ns app.core
  (:require [reagent.dom :as r]
            [app.hello-form]))

(defn ^:dev/after-load render []
  (r/render [app.hello-form/form] (.getElementById js/document "app")))

(defn ^:export main []
  (render))
