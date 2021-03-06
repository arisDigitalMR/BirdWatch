(ns birdwatch.ui
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [birdwatch.util :as util]
            [birdwatch.channels :as c]
            [birdwatch.communicator :as comm]
            [birdwatch.state :as state]
            [cljs.core.async :as async :refer [<! chan put!]]))

(enable-console-print!)

(defn count-view
  "rendering tweet counter"
  [app owner]
  (reify
    om/IRender
    (render [this] (dom/span nil (:count app)))))

(defn users-count-view
  "rendering users counter"
  [app owner]
  (reify
    om/IRender
    (render [this]
            (let [users (:users-count app)]
              (dom/span nil "Connected: " (dom/strong nil users) (if (= users 1) " user" " users"))))))

(defn total-count-view
  "rendering total tweets counter"
  [app owner]
  (reify
    om/IRender
    (render [this]
            (dom/span nil "Indexed: " (dom/strong nil (:total-tweet-count app)) " tweets"))))

(defn sort-button-js
  "generates JS for sort button for both updating sort order and showing active button"
  [app key]
  #js {:onClick (fn [e] (om/update! app [:sorted] key))
       :className (str "pure-button not-rounded" (if (= key (:sorted app)) " pure-button-primary" " sort-button"))})

(defn sort-buttons-view
  "rendering sort buttons"
  [app owner]
  (reify
    om/IRender
    (render [this]
            (dom/div nil
                     (dom/button #js {:className "pure-button not-rounded sort-button"} "Sort by")
                     (dom/button (sort-button-js app :by-id) "latest")
                     (dom/button (sort-button-js app :by-followers) "followers")
                     (dom/button (sort-button-js app :by-retweets) "retweets")
                     (dom/button (sort-button-js app :by-rt-since-startup) "retweets2")
                     (dom/button (sort-button-js app :by-reach) "reach")
                     (dom/button (sort-button-js app :by-favorites) "favorites")))))

(defn handle-search-change [e app]
  (swap! state/app assoc :search-text (.. e -target -value)))

(defn search-view
  "rendering search bar"
  [app owner]
  (reify
    om/IRender
    (render [this]
            (dom/form #js {:className "pure-form"}
                      (dom/fieldset nil
                               (dom/input #js {:type "text"
                                               :value (:search-text (om/get-props owner))
                                               :placeholder "Example search: java (job OR jobs OR hiring)"
                                               :onKeyPress #(when (== (.-keyCode %) 13) (comm/start-search))
                                               :onChange #(handle-search-change % app)})
                               (dom/button #js {:className "pure-button pure-button-primary" :onClick #(comm/start-search)}
                                           (dom/span #js {:className "glyphicon glyphicon-search"})))))))

(defn pag-items
  "function creating pagination items"
  [app page-change-chan]
  (map #(dom/button #js {:className (str "pure-button not-rounded button-xsmall" (if (= % (:page app)) " pure-button-primary" ""))
                         :onClick (fn [e] (put! page-change-chan %))} %)
       (take 15 (range 1 (Math/floor (/ (:count app) (:n app)))))))

(defn pagination-view
  "rendering pagination list"
  [app owner]
  (reify
    om/IInitState
    (init-state [_]
                {:page-change (chan)})
    om/IWillMount
    (will-mount [_]
                (let [page-change (om/get-state owner :page-change)]
                  (go (loop []
                        (let [page (<! page-change)]
                          (om/update! app :page page)
                          (recur))))))
    om/IRenderState
    (render-state [this {:keys [page-change]}]
                  (apply dom/div nil
                         (flatten [(dom/button #js {:className "pure-button not-rounded button-xsmall"} "Page")
                                   (pag-items app page-change)])))))
