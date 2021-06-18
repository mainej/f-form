(ns app.hello-form
  (:require [f-form.dom :as form.dom]
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
      active?                          "←"
      (not touched?)                   ""
      (or (seq errors) (seq warnings)) "x"
      :else                            "✓")]
   [:div.space-y-1
    [errors-list touched? :text-yellow warnings]
    [errors-list touched? :text-red errors]]])

(def field-labels
  {[:full-name]             "Full Name"
   [:security-question]     "Security Question"
   [:security-response]     "Security Question Response"
   [:password]              "Password"
   [:password-confirmation] "Password Confirmation"
   [:premium-account]       "Premium Account"})

(defn field-label [field]
  (get field-labels (:field/path field)))

(def security-questions
  [{:question/id   "food"
    :question/text "What is your favorite food?"}
   {:question/id   "pet"
    :question/text "What is the name of your first pet?"}])

(defn validation [form]
  (vlad/join
   (form.vlad/field [:full-name] (form.vlad/non-nil))
   (form.vlad/field [:security-question] (form.vlad/non-nil))
   (form.vlad/field [:security-response] (form.vlad/non-nil))
   (vlad/chain
    (form.vlad/field [:password] (vlad/length-over 7))
    (form.vlad/field [:password-confirmation] (vlad/equals-value
                                               (form/value-by-path form [:password])
                                               {:type        ::vlad/equals-field
                                                :first-name  (field-labels [:password-confirmation])
                                                :second-name (field-labels [:password])})))
   (form.vlad/field [:premium-account]
                    (form.vlad/warning
                     (vlad/predicate
                      #(not (true? %))
                      {:message (str "You need a " (field-labels [:premium-account]) " to access all features.")})))))

(defn validate-form [form]
  (form.vlad/validate form (validation form) field-labels))

(def tracker (form.field-tracker/tracker #{:field/active? :field/touched?}))

(def hello-form
  (r/atom (validate-form
           (form/init [(field/init [:full-name] "John Doe" tracker)
                       (field/init [:security-question] nil tracker)
                       (field/init [:security-response] nil tracker)
                       (field/init [:password] nil tracker)
                       (field/init [:password-confirmation] nil tracker)
                       (field/init [:premium-account] false tracker)]))))

(defn update-field [path update-fn _dom-event]
  (swap! hello-form (fn [form]
                      (validate-form (form/update-field-by-path form path update-fn)))))

(defn basic-input [props field]
  [:<>
   [:label {:for (:id props)} [:span (field-label field)]]
   [:div
    [:input.w-full
     (form.dom/input (assoc props :on-change update-field)
                     field)]]
   [field-status field]])

(defn password-input [props field]
  [basic-input (assoc props :type "password") field])

(defn security-question-input [field]
  [:<>
   [:label {:for "field-question"} [:span (field-label field)]]
   [:div
    [:select#field-question
     (f-form.dom/select {:on-change update-field}
                        field
                        {:options      security-questions
                         :option-value :question/id})
     [:option {:value "" :disabled true} "Choose..."]
     (for [{:keys [question/id question/text]} security-questions]
       ^{:key id}
       [:option {:value id} text])]]
   [field-status field]])

(defn premium-account-input [field]
  [:<>
   [:label {:for "field-premium-account-acceptance"} [:span (field-label field)]]
   [:div
    [:input#field-premium-account-acceptance
     (form.dom/checkbox {:on-change update-field}
                        field)]]
   [field-status field]])

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
       [basic-input {:id "field-full-name", :auto-focus true} (form/field-by-path form [:full-name])]
       [security-question-input (form/field-by-path form [:security-question])]
       [basic-input {:id "field-response"} (form/field-by-path form [:security-response])]
       [password-input {:id "field-password"} (form/field-by-path form [:password])]
       [password-input {:id "field-password-confirmation"} (form/field-by-path form [:password-confirmation])]
       [premium-account-input (form/field-by-path form [:premium-account])]]
      [:button {:type     "submit"
                :disabled (form.validation/invalid? form)}
       "Submit"]]
     [:code.block (pr-str (form/values form))]]))
