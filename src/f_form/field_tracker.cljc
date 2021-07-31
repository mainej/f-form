(ns f-form.field-tracker
  "Tools to help a [[f-form.field]] accumulate history about how it has been
  interacted with.

  There are two classes of interaction history.

  First, the field can gain or lose focus. This history is summarized with three
  attributes:

  * `:field/visited?` - whether the field has ever received focus. Typically
    used to mark off complete fields or to call attention to incomplete or pending
    fields.
  * `:field/active?` - whether the field currently has focus. Typically used to
    add an outline or some other emphasis to the focused field, if this cannot be
    done with CSS.
  * `:field/touched?` - whether the field has ever lost focus. Typically used to
    hide errors until the user has failed to take the opportunity to resolve them, or
    to show a ✓ or some other indicator beside completed fields.

  Second, its value can be changed. This history is summarized with two
  attributes:

  * `:field/modified?` - whether the field's value has ever been changed (by a
    user action). Typically used to allow the system to control the value of a
    field until the user makes a choice, then to hand control of the value over to
    them.
  * `:field/pristine?` - whether the field's value is the same as its initial
    value. Typically used to skip submission of unchanged fields.

  For these two classes of interaction history, there are two sets of
  interaction events which modify the history.

  The events related to the focus state of the field are:

  * `:initialized`, when the field is created or reset.
  * `:focus-gained`, when the field gains focus.
  * `:focus-lost`, when the field loses focus.

  The events related to the value of the field are:

  * `:changed`, when the field's value changes.
  * `:snapshot`, when the field's value and initial-value are synced, for example
    when the field is initialized or when its form has been submitted on a
    periodic timer.

  The names of the history fields were influenced by Final Form
  [FieldState](https://final-form.org/docs/final-form/types/FieldState).

  Since not every application design will care about every state change, and
  since there is a cost to tracking excess state (computationally when
  calculating the state, and in some cases when triggering unnecessary updates
  to a datastore) it is best to skip accumulation of unused state. This
  namespace provides some tools to customize a [[tracker]] that helps the field
  accumulate a minimal amount of state.")

(defn tracker
  "Builds a tracker that tracks state for the given `tracking-attrs`, a set of
  some or all of:

  * `:field/visited?`
  * `:field/active?`
  * `:field/touched?`
  * `:field/modified?`
  * `:field/pristine?`

  Most applications will need only one tracker, which can be applied to all
  fields. By default the [[default-tracker]] will be applied."
  [tracking-attrs]
  (set tracking-attrs))

(def full-tracker
  "A tracker which tracks all five common pieces of state."
  (tracker #{:field/visited?
             :field/active?
             :field/touched?
             :field/modified?
             :field/pristine?}))

(def default-tracker
  "A tracker which tracks only one of the five common pieces of state - `:field/touched?`."
  (tracker #{:field/touched?}))

(defn pristine?
  "Whether the field's current value is the same as its initial value."
  [field]
  (= (:field/value field) (:field/initial-value field)))

(def ^:private transitions
  "A map of transitions: tracking-attr -> event -> transition.
  In English: given we are tracking `tracking-attr`, when we see `event`, then
  apply `transition` to the field, to get the new value for the `tracking-attr`."
  {:field/visited?  {:initialized  (constantly false)
                     :focus-gained (constantly true)}
   :field/active?   {:initialized  (constantly false)
                     :focus-gained (constantly true)
                     :focus-lost   (constantly false)}
   :field/touched?  {:initialized (constantly false)
                     :focus-lost  (constantly true)}
   :field/modified? {:snapshot (constantly false)
                     :changed  (constantly true)}
   :field/pristine? {:snapshot (constantly true)
                     :changed  pristine?}})

(defn track
  "Track the `event` on the `field`, using the tracker configured on the field.
  Returns the `field` with new history tracked on it."
  [field event]
  (persistent!
   (reduce (fn [field tracking-attr]
             (if-let [transition (get-in transitions [tracking-attr event])]
               (assoc! field tracking-attr (transition field))
               field))
           (transient field)
           (:field/tracker field))))
