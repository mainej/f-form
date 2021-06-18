(ns app.hello-form
  (:require [clojure.string :as string]
            [f-form.dom :as form.dom]
            [f-form.field :as field]
            [f-form.form :as form]
            [f-form.field-tracker :as form.field-tracker]
            [f-form.validation :as form.validation]
            [f-form.validation.vlad :as form.vlad]
            [reagent.core :as r]
            [vlad.core :as vlad]))

(defn errors-list
  [shown? color errors]
  (when (seq errors)
    [:ul.space-y-1
     {:class [color (when (not shown?) :invisible)]}
     (for [[i error] (map-indexed vector errors)]
       ^{:key i}
       [:li error])]))

(defn field-status [{:keys [field/active? field/touched? field/errors field/warnings]}]
  [:div.flex.space-x-1
   [:span.w-4 {:class (cond
                        (seq errors)   :text-red
                        (seq warnings) :text-yellow
                        :else          :text-green)}
    (cond
      ;; EXAMPLE: use :field/active? to highlight current field
      active?                          "←"
      ;; EXAMPLE: use :field/touched? to delay error feedback until after interaction (1)
      (not touched?)                   ""
      (or (seq errors) (seq warnings)) "x"
      :else                            "✓")]
   [:div.space-y-1
    ;; EXAMPLE: use :field/touched? to delay error feedback until after interaction (2)
    [errors-list touched? :text-yellow warnings]
    ;; EXAMPLE: non-blocking warnings (1)
    [errors-list touched? :text-red errors]]])

(def field-labels
  {[:full-name]             "Full Name"
   [:communication-style]   "Preferred Communication Style"
   [:password]              "Password"
   [:password-confirmation] "Password Confirmation"
   [:premium-account]       "Premium Account"})

(defn field-label [field]
  (get field-labels (:field/path field)))

(def communication-styles
  [{:comms.style/id   "email"
    :comms.style/text "Email me"}
   {:comms.style/id   "sms"
    :comms.style/text "Text me"}
   {:comms.style/id   "phone"
    :comms.style/text "Call me"}])

(defn validation [form]
  (vlad/join
   (form.vlad/field [:full-name] (form.vlad/non-nil))
   (form.vlad/field [:communication-style] (form.vlad/non-nil))
   (vlad/chain
    (form.vlad/field [:password] (vlad/length-over 7))
    ;; EXAMPLE: multi-field validation
    (form.vlad/field [:password-confirmation] (vlad/equals-value
                                               (form/value-by-path form [:password])
                                               {:type        ::vlad/equals-field
                                                :first-name  (field-labels [:password-confirmation])
                                                :second-name (field-labels [:password])})))
   (form.vlad/field [:premium-account]
                    ;; EXAMPLE: non-blocking warnings (2)
                    (form.vlad/warning
                     (vlad/predicate
                      #(not (true? %))
                      ;; EXAMPLE: custom error messages
                      {:message (str "You need a " (field-labels [:premium-account]) " to access all features.")})))))

(defn validate-form [form]
  (form.vlad/validate form (validation form) field-labels))

;; EXAMPLE: expanded tracking
;;
;; We wouldn't have to pass a tracker to field/init if we were OK with the
;; default tracker, but in this case, since we want to track :field/active?, we
;; need a custom one.
(def tracker (form.field-tracker/tracker #{:field/active? :field/touched?}))

(def hello-form
  ;; EXAMPLE: external state management (initial)
  (r/atom
   ;; EXAMPLE: validation (initial)
   (validate-form
    (form/init [(field/init [:full-name] "John Doe" tracker)
                (field/init [:communication-style] nil tracker)
                (field/init [:password] nil tracker)
                (field/init [:password-confirmation] nil tracker)
                (field/init [:premium-account] false tracker)]))))

(defn update-field [path update-fn _dom-event]
  ;; EXAMPLE: external state management (on change)
  (swap! hello-form (fn [form]
                      ;; EXAMPLE: validation (on change)
                      (validate-form (form/update-field-by-path form path update-fn)))))

(defn element-id [{:keys [field/path]}]
  (string/join "-" path))

;; EXAMPLE: html and css at user discretion (1)
(defn basic-input
  ([field] [basic-input {} field])
  ([props field]
   (let [id (element-id field)]
     [:<>
      [:label {:for id} [:span (field-label field)]]
      [:div
       [:input.w-full
        ;; EXAMPLE: event handlers for an <input> element and a string-valued field
        (form.dom/input (assoc props :id id, :on-change update-field)
                        field)]]
      [field-status field]])))

(defn password-input
  ([field] [password-input {} field])
  ([props field]
   [basic-input (assoc props :type "password") field]))

;; EXAMPLE: html and css at user discretion (2)
(defn checkbox-input
  ([field] [checkbox-input {} field])
  ([props field]
   (let [id (element-id field)]
     [:<>
      [:label {:for id} [:span (field-label field)]]
      [:div
       [:input
        ;; EXAMPLE: event handlers for a <input type="checkbox"> element and a boolean-valued field
        (form.dom/checkbox (assoc props :id id, :on-change update-field)
                           field)]]
      [field-status field]])))

;; EXAMPLE: html and css at user discretion (3)
(defn basic-select
  ([field config] [basic-select {} field config])
  ([props field {:keys [options option-value option-body]}]
   (let [id (element-id field)]
     [:<>
      [:label {:for id} [:span (field-label field)]]
      [:div
       [:select
        ;; EXAMPLE: event handlers for a <select> element and a complex-valued field
        (f-form.dom/select (assoc props :id id, :on-change update-field)
                           field
                           {:options      options
                            :option-value option-value})
        [:option {:value "" :disabled true} "Choose..."]
        (for [option options
              :let   [value (option-value option)]]
          ^{:key value}
          [:option {:value value} (option-body option)])]]
      [field-status field]])))

;; EXAMPLE: input customized for one field
(defn communication-style-input [field]
  [basic-select field {:options      communication-styles
                       :option-value :comms.style/id
                       :option-body  :comms.style/text}])

(defn form []
  (let [form @hello-form]
    [:div.max-w-5xl.space-y-4
     [:h1 "Welcome"]
     [:p.space-y-4
      [:code.block
       "Note to developers: Field errors exist in the page but are hidden until
       each field is visited, so that users aren't met with a wall of red text.
       To expose the errors, tab through the fields."]
      [:code.block
       "When all errors (though not necessarily all warnings) have been
       resolved, the submit button will become enabled."]]
     [:form.space-y-4 {:on-submit (fn [e] (.preventDefault e))}
      [:div.grid.grid-cols-1.md:grid-cols-3.gap-1.md:gap-4
       [basic-input {:auto-focus true} (form/field-by-path form [:full-name])]
       [communication-style-input (form/field-by-path form [:communication-style])]
       [password-input (form/field-by-path form [:password])]
       [password-input (form/field-by-path form [:password-confirmation])]
       [checkbox-input (form/field-by-path form [:premium-account])]]
      [:button {:type     "submit"
                ;; EXAMPLE: rendezvous for all validation
                :disabled (form.validation/invalid? form)}
       "Submit"]]
     [:code.block (pr-str (form/values form))]]))
