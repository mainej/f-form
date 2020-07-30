(ns f-form.field-tracker
  "As events occur in the lifecycle of a field, its tracker accumulates its current
  state or history.

  The lifecycle events are:
  * `:initialized`, when the field is created or reset
  * `:snapshot` when the field's value and initial-value are synced (and which is
  also called on `:initialize`)
  * `:focus-gained` when the field gains focus
  * `:focus-lost`, when the field loses focus
  * `:changed`, when the field's value changes

  Custom trackers can be built to respond to these lifecycle events in arbitrary
  ways. However, most apps will use a tracker that accumulates some or all of
  the following state:

  * `:field/visited?` - whether the field has ever received focus. Typically
  used to call attention to incomplete or pending fields.
  * `:field/active?` - whether the field currently has focus. Typically used to
  add an outline or some other emphasis to the focused field, if this cannot be
  done with CSS.
  * `:field/touched?` - whether the field has ever lost focus. Typically used to
  hide errors until the user has failed to take the opportunity to resolve them, or
  to show a ✓ or some other indicator beside completed fields.
  * `:field/modified?` - whether the field's value has ever been changed (by a
  user action). Typically used to allow the system to control the value of a
  field until the user makes a choice, then to hand control of the value over to
  them.
  * `:field/pristine?` - whether the field's value is the same as its initial
  value. Typically used to skip submission of unchanged fields.

  Since not every application design will care about every state change, and
  since there is a cost to tracking excess state (computationally when
  calculating the state, and in some cases when triggering unnecessary updates
  to a datastore) it is best to skip accumulation of unused state. This
  namespace provides some tools to customize a tracker that accumulates a
  minimal amount of state.")

(defn- comp-some [& fns]
  (apply comp (remove nil? fns)))

(defn tracker
  "Builds a tracker that tracks state for the given `tracked-fields`, a set of
  some or all of:

  * `:field/visited?`
  * `:field/active`
  * `:field/touched?`
  * `:field/modified?`
  * `:field/pristine?`

  Most applications will need only one tracker, which can be applied to all
  fields. By default the [[default-tracker]] will be applied.

  Tries to build an efficient tracker by pre-calculating the functions that will
  modify the field with every change. Has not been benchmarked against other
  approaches."
  [tracked-fields]
  {:initialized  (comp-some (when (tracked-fields :field/visited?)  #(assoc % :field/visited? false))
                            (when (tracked-fields :field/active?)   #(assoc % :field/active? false))
                            (when (tracked-fields :field/touched?)  #(assoc % :field/touched? false)))
   :snapshot     (comp-some (when (tracked-fields :field/modified?) #(assoc % :field/modified? false))
                            (when (tracked-fields :field/pristine?) #(assoc % :field/pristine? true)))
   :focus-gained (comp-some (when (tracked-fields :field/visited?)  #(assoc % :field/visited? true))
                            (when (tracked-fields :field/active?)   #(assoc % :field/active? true)))
   :focus-lost   (comp-some (when (tracked-fields :field/active?)   #(assoc % :field/active? false))
                            (when (tracked-fields :field/touched?)  #(assoc % :field/touched? true)))
   :changed      (comp-some (when (tracked-fields :field/modified?) #(assoc % :field/modified? true))
                            (when (tracked-fields :field/pristine?) #(assoc % :field/pristine? (= (:field/value %) (:field/initial-value %)))))})

(def default-tracker
  "A tracker which tracks only one of the five common pieces of state - `:field/touched?`."
  (tracker #{#_:field/visited?
             #_:field/active?
             :field/touched?
             #_:field/modified?
             #_:field/pristine?}))

(defn track
  "Track the `event` on the `field`, using the tracker configured on the field.
  Returns a `field` with new state tracked on it."
  [field event]
  (let [f (get-in field [:field/tracker event])]
    (f field)))
