(ns app.hello-form
  "An example of f-form working with reagent.

  See the README.md for instructions on running this code.

  While reviewing, keep an eye out for EXAMPLE annotations. They highlight
  certain pieces of f-form functionality."
  (:require [clojure.string :as string]
            [f-form.dom :as form.dom]
            [f-form.field :as field]
            [f-form.form :as form]
            [f-form.field-tracker :as form.field-tracker]
            [f-form.validation :as form.validation]
            [f-form.validation.vlad :as form.vlad]
            [reagent.core :as r]
            [vlad.core :as vlad]))

;;;; DATA & DEFINITIONS

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

;; EXAMPLE: dynamic validation
(defn validation [form]
  (vlad/join
   (form.vlad/field [:full-name] (form.vlad/non-nil))
   (form.vlad/field [:communication-style] (form.vlad/non-nil))
   (form.vlad/field [:password] (vlad/length-over 7))
   ;; EXAMPLE: multi-field validation
   (form.vlad/field [:password-confirmation] (vlad/equals-value
                                              (form/value-by-path form [:password])
                                              {:type        ::vlad/equals-field
                                               :first-name  (field-labels [:password-confirmation])
                                               :second-name (field-labels [:password])}))
   (form.vlad/field [:premium-account]
                    ;; EXAMPLE: non-blocking warnings (1)
                    (form.vlad/warning
                     (vlad/predicate
                      #(not (true? %))
                      ;; EXAMPLE: custom error messages
                      {:message (str "You need a " (field-labels [:premium-account]) " to access all features.")})))))

;;;; STATE MANAGEMENT & VALIDATION

;; EXAMPLE: If this were re-frame, this section would be several event handler
;;          and subscription registrations: `re-frame.core/reg-event-*` and
;;          `re-frame.core/reg-sub`.

(defn validate-form [form]
  (form.vlad/validate form (validation form) field-labels))

(defonce hello-form
  ;; EXAMPLE: external state management (initial)
  ;; EXAMPLE: validation (initial)
  (r/atom (validate-form (new-form))))

(defn field-by-path! [path]
  (form/field-by-path @hello-form path))

(defn update-field! [path update-fn _dom-event]
  ;; EXAMPLE: external state management (on change)
  (swap! hello-form (fn [form]
                      ;; EXAMPLE: validation (on change)
                      (validate-form (form/update-field-by-path form path update-fn)))))

(defn- update-fields [form update-fn]
  (reduce (fn [form {:keys [field/path]}]
            (form/update-field-by-path form path update-fn))
          form
          (form/fields form)))

(defn invalid-values []
  (form/values @hello-form (filter #(seq (:field/errors %)))))

(defn valid-values []
  (form/values @hello-form (remove #(seq (:field/errors %)))))

(defn show-validation? []
  (:form/show-validation? @hello-form))

(defn show-disabled-warning? []
  (and (show-validation?)
       (form.validation/invalid? @hello-form)))

(defn submitting? []
  (form/submitting? @hello-form))

(defn submitted! []
  (swap! hello-form
         (fn [form]
           (-> form
               ;; Re-enable submit button. It's important to do this when a
               ;; submission fails on the server so the user has a chance to
               ;; re-submit. If the submissions succeeds on the server you may
               ;; not need it, because you will likely be discarding the whole
               ;; form.
               form/submitted
               ;; Example: Normally you wouldn't do this... for the purposes of
               ;; the example, discard the fields' history, as if you had
               ;; arrived at this page with the data you have just submitted.
               (update-fields field/discard-history)))))

(defn mock-submit! []
  (if (form.validation/valid? @hello-form)
    (do
      (swap! hello-form (fn [form]
                          (-> form
                              form/submitting
                              (dissoc :form/show-validation?))))
      (js/setTimeout submitted! 2000))
    (swap! hello-form assoc :form/show-validation? true)))

;;;; HTML UTILS

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
                                  ;; NOTE: all field changes go through this one handler
                                  :on-change update-field!)
    (field-invalid? field) (assoc :aria-invalid true
                                  :aria-describedby (errors-id field))))

;;;; HTML WRAPPERS

(defn errors-list
  [touched? color errors]
  (when (seq errors)
    [:ul.space-y-1
     {:class [color (when-not (or touched? (show-validation?))
                      :sr-only)]}
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
    [errors-list touched? :text-red errors]
    ;; EXAMPLE: non-blocking warnings (2)
    [errors-list touched? :text-yellow warnings]]])

(defn field-label [{:keys [field/path] :as field}]
  [:label {:for (element-id field)} [:span (get field-labels path)]])

(defn fieldgroup
  "A fieldgroup is a row with 3 columns: A label, an input, and some error messages."
  [field child-input]
  [:<>
   [field-label field]
   [:div child-input]
   [field-status field]])

;;;; HTML INPUT COMPONENTS

(defn generic-input
  ([field] [generic-input {} field])
  ([props field]
   [fieldgroup field
    ;; EXAMPLE: html and css at user discretion. In this case, inputs are full
    ;;          width, but checkboxes are not.
    ;; EXAMPLE: event handlers for an <input> element and a string-valued field
    [:input.w-full (form.dom/input (field-props props field) field)]]))

(defn generic-password-input
  ([field] [generic-password-input {} field])
  ([props field]
   [generic-input (assoc props :type "password") field]))

(defn generic-checkbox-input
  ([field] [generic-checkbox-input {} field])
  ([props field]
   [fieldgroup field
    ;; EXAMPLE: event handlers for a <input type="checkbox"> element and a boolean-valued field
    [:input (form.dom/checkbox (field-props props field) field)]]))

(defn generic-select
  ([field config] [generic-select {} field config])
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

;;;; HTML PAGE & FORM COMPONENTS

;; EXAMPLE: inputs customized for one field

(defn full-name-input []
  [generic-input {:auto-focus true} (field-by-path! [:full-name])])

(defn communication-style-input []
  [generic-select (field-by-path! [:communication-style])
   {:options      communication-styles
    :option-value :comms.style/id
    :option-body  :comms.style/text}])

(defn password-input []
  [generic-password-input (field-by-path! [:password])])

(defn password-confirmation-input []
  [generic-password-input (field-by-path! [:password-confirmation])])

(defn premium-account-input []
  [generic-checkbox-input (field-by-path! [:premium-account])])

(defn submit-button []
  [:div
   (when (show-disabled-warning?)
     [:p.text-red "Please fix errors."])
   [:button {:type     "submit"
             :disabled (submitting?)}
    "Submit" (when (submitting?) "...")]])

(defn debug-invalid-values []
  [:div
   [:h2 "Invalid"]
   [:code.overflow-x-auto (pr-str (invalid-values))]])

(defn debug-valid-values []
  [:div
   [:h2 "Valid"]
   [:code.overflow-x-auto (pr-str (valid-values))]])

(defn form []
  [:main.max-w-5xl.space-y-4
   [:h1 "Welcome"]
   [:p.space-y-4
    [:code.block
     "Note to developers: Field errors exist in the page but are hidden until
       each field is visited, so that users aren't met with a wall of red text.
       To expose the errors, tab through the fields, or click submit."]]
   [:div.space-y-4
    ;; EXAMPLE: If a form has many fields, it's better to subscribe to each one
    ;; in its own component, rather than to the form as a whole, to avoid
    ;; re-rendering every field whenever one of them has any interaction.
    [:form.space-y-4 {:on-submit (fn [e]
                                   (.stopPropagation e)
                                   (.preventDefault e)
                                   (mock-submit!))}
     [:div.grid.grid-cols-1.md:grid-cols-3.gap-1.md:gap-4
      [full-name-input]
      [communication-style-input]
      [password-input]
      [password-confirmation-input]
      [premium-account-input]]
     [submit-button]]
    [debug-invalid-values]
    [debug-valid-values]]])
