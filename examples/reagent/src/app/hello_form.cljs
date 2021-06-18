(ns app.hello-form
  (:require [f-form.dom :as form.dom]
            [f-form.field :as field]
            [f-form.form :as form]
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

(defn errors
  [{:keys [field/touched? field/errors field/warnings]}]
  (when (or (seq errors) (seq warnings))
    [:div.space-y-1
     [errors-list touched? :text-yellow warnings]
     [errors-list touched? :text-red errors]]))

(def field-labels
  {[:full-name]             "Full Name"
   [:security-question]     "Security Question"
   [:security-response]     "Response"
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

(def hello-form
  (r/atom (validate-form
           (form/init [(field/init [:full-name] "John Doe")
                       (field/init [:security-question])
                       (field/init [:security-response])
                       (field/init [:password])
                       (field/init [:password-confirmation])
                       (field/init [:premium-account] false)]))))

(defn update-field [path update-fn _dom-event]
  (swap! hello-form (fn [form]
                      (validate-form (form/update-field-by-path form path update-fn)))))

(defn basic-input [props field]
  [:<>
   [:label {:for (:id props)} [:span (field-label field)]]
   [:div.flex.flex-column.space-y-1
    [:input
     (form.dom/input (assoc props :on-change update-field)
                     field)]
    [errors field]]])

(defn password-input [props field]
  [basic-input (assoc props :type "password") field])

(defn security-question-input [question-field response-field]
  [:<>
   [:label {:for "field-question"} [:span (field-label question-field)]]
   [:div]
   [:div.flex.flex-column.space-y-1
    [:select#field-question
     (f-form.dom/select {:on-change update-field}
                        question-field
                        {:options      security-questions
                         :option-value :question/id})
     [:option {:value "" :disabled true} "Choose..."]
     (for [{:keys [question/id question/text]} security-questions]
       ^{:key id}
       [:option {:value id} text])]
    [errors question-field]]
   [:div.flex.flex-column.space-y-1
    [:label [:span.sr-only (field-label response-field)]
     [:input
      (form.dom/input {:on-change update-field}
                      response-field)]]
    [errors response-field]]])

(defn premium-account-input [field]
  [:<>
   [:label {:for "field-premium-account-acceptance"} [:span (field-label field)]]
   [:div.space-y-1
    [:input#field-premium-account-acceptance
     (form.dom/checkbox {:on-change update-field}
                        field)]
    [errors field]]])

(defn form []
  (let [form @hello-form]
    [:div.max-w-md.space-y-4
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
      [:div.grid.grid-cols-2.gap-4
       [basic-input {:id "field-full-name"} (form/field-by-path form [:full-name])]
       [security-question-input
        (form/field-by-path form [:security-question])
        (form/field-by-path form [:security-response])]
       [password-input {:id "field-password"} (form/field-by-path form [:password])]
       [password-input {:id "field-password-confirmation"} (form/field-by-path form [:password-confirmation])]
       [premium-account-input (form/field-by-path form [:premium-account])]]
      [:button {:type     "submit"
                :disabled (form.validation/invalid? form)}
       "Submit"]]
     [:code.block (pr-str (form/values form))]]))
