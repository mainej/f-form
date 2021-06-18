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

;; DATA & DEFINITIONS

(def field-labels
  {[:full-name]             "Full Name"
   [:communication-style]   "Preferred Communication Style"
   [:password]              "Password"
   [:password-confirmation] "Password Confirmation"
   [:premium-account]       "Premium Account"})

(def communication-styles
  [{:comms.style/id   "email"
    :comms.style/text "Email me"}
   {:comms.style/id   "sms"
    :comms.style/text "Text me"}
   {:comms.style/id   "phone"
    :comms.style/text "Call me"}])

;; EXAMPLE: dynamic validation
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

(defn new-form []
  ;; EXAMPLE: expanded tracking
  ;;
  ;; We wouldn't have to pass a tracker to field/init if we were OK with the
  ;; default tracker, but in this case, since we want to track :field/active?, we
  ;; need a custom one.
  (let [tracker (form.field-tracker/tracker #{:field/active? :field/touched?})]
    ;; EXAMPLE: form and field definition
    (form/init [(field/init [:full-name] "John Doe" tracker)
                (field/init [:communication-style] nil tracker)
                (field/init [:password] nil tracker)
                (field/init [:password-confirmation] nil tracker)
                (field/init [:premium-account] false tracker)])))

;; STATE MANAGEMENT & VALIDATION

(defn validate-form [form]
  (form.vlad/validate form (validation form) field-labels))

(def hello-form
  ;; EXAMPLE: external state management (initial)
  ;; EXAMPLE: validation (initial)
  (r/atom (validate-form (new-form))))

(defn update-field [path update-fn _dom-event]
  ;; EXAMPLE: external state management (on change)
  (swap! hello-form (fn [form]
                      ;; EXAMPLE: validation (on change)
                      (validate-form (form/update-field-by-path form path update-fn)))))

;; HTML UTILS

(defn element-id [{:keys [field/path]}]
  (str "element-" (string/join "-" path)))

(defn errors-id [{:keys [field/path]}]
  (str "errors-" (string/join "-" path)))

(defn field-invalid? [{:keys [field/errors field/warnings]}]
  (or (seq errors) (seq warnings)))

(defn field-props
  "Extend input `props` (as per f-form.dom) with data relevant for this `field`"
  [props field]
  (cond-> props
    :always                (assoc :id (element-id field)
                                  :on-change update-field)
    (field-invalid? field) (assoc :aria-invalid true
                                  :aria-describedby (errors-id field))))

;; HTML WRAPPERS

(defn errors-list
  [shown? color errors]
  (when (seq errors)
    [:ul.space-y-1
     {:class [color (when (not shown?) :sr-only)]}
     (for [[i error] (map-indexed vector errors)]
       ^{:key i}
       [:li error])]))

(defn field-status [{:keys [field/active? field/touched? field/errors field/warnings] :as field}]
  [:div.flex.space-x-1
   [:span.w-4 {:class       (cond
                              (seq errors)   :text-red
                              (seq warnings) :text-yellow
                              :else          :text-green)
               :aria-hidden true}
    (cond
      ;; EXAMPLE: use :field/active? to highlight current field
      active?                "←"
      ;; EXAMPLE: use :field/touched? to delay error feedback until after interaction (1)
      (not touched?)         ""
      (field-invalid? field) "x"
      :else                  "✓")]
   [:div.space-y-1 {:id (errors-id field)}
    ;; EXAMPLE: use :field/touched? to delay error feedback until after interaction (2)
    [errors-list touched? :text-yellow warnings]
    ;; EXAMPLE: non-blocking warnings (1)
    [errors-list touched? :text-red errors]]])

(defn field-label [{:keys [field/path] :as field}]
  [:label {:for (element-id field)} [:span (get field-labels path)]])

(defn fieldgroup
  "A fieldgroup is a row with 3 columns: A label, an input, and some error messages."
  [field child-input]
  [:<>
   [field-label field]
   [:div child-input]
   [field-status field]])

;; HTML INPUT COMPONENTS

(defn basic-input
  ([field] [basic-input {} field])
  ([props field]
   [fieldgroup field
    ;; EXAMPLE: html and css at user discretion. In this case, inputs are full
    ;;          width, but checkboxes are not.
    ;; EXAMPLE: event handlers for an <input> element and a string-valued field
    [:input.w-full (form.dom/input (field-props props field) field)]]))

(defn password-input
  ([field] [password-input {} field])
  ([props field]
   [basic-input (assoc props :type "password") field]))

(defn checkbox-input
  ([field] [checkbox-input {} field])
  ([props field]
   [fieldgroup field
    ;; EXAMPLE: event handlers for a <input type="checkbox"> element and a boolean-valued field
    [:input (form.dom/checkbox (field-props props field) field)]]))

(defn basic-select
  ([field config] [basic-select {} field config])
  ([props field {:keys [options option-value option-body]}]
   [fieldgroup field
    ;; EXAMPLE: event handlers for a <select> element and a complex-valued field
    [:select
     (f-form.dom/select (field-props props field)
                        field
                        {:options      options
                         :option-value option-value})
     [:option {:value "" :disabled true} "Choose..."]
     (for [option options
           :let   [value (option-value option)]]
       ^{:key value}
       [:option {:value value} (option-body option)])]]))

;; EXAMPLE: input customized for one field
(defn communication-style-input [field]
  [basic-select field {:options      communication-styles
                       :option-value :comms.style/id
                       :option-body  :comms.style/text}])

;; HTML PAGE & FORM COMPONENTS

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
     [:div
      [:h2 "Invalid"]
      [:code.block (pr-str (form/values form (filter #(seq (:field/errors %)))))]]
     [:div
      [:h2 "Valid"]
      [:code.block (pr-str (form/values form (remove #(seq (:field/errors %)))))]]]))
