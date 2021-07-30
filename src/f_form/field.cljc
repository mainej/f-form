(ns f-form.field
  "Functions for creating and updating a `field`, an immutable representation of
  a single element in a form. A field has a value, some history about interactions
  with it, and knowledge about where it fits in its [[f-form.form]].

  Conceptually, a `field` corresponds to a single HTML `<input>`, `<select>`, or
  `<textarea>`.

  A field has a `:field/value`, which can be any value: a primitive string, number or
  boolean, or a more elaborate map or set. (It also has a `:field/initial-value`,
  the value before any changes.)

  It has a `:field/path`, which uniquely identifies it in its form. The path is
  a sequence of keys as defined by `clojure.core/assoc-in`.

  And finally, it has a `:field/tracker`. The tracker defines which other state
  and history attributes will be stored on the field. See
  [[f-form.field-tracker]] for more information about these attributes and how
  they are set.

  See [[f-form.dom]] for tools to attach a field to a DOM node."
  (:require [f-form.field-tracker :as tracker]))

(defn discard-interaction-history
  "Discard history established by the tracker about the `field`'s focus state."
  [field]
  (tracker/track field :initialized))

(defn discard-value-history
  "Discard history established by the tracker about the `field`'s value."
  [field]
  (tracker/track field :snapshot))

(defn discard-history
  "Discard all history established by the tracker about the `field`."
  [field]
  (-> field (discard-value-history) (discard-interaction-history)))

(defn reset-value
  "Revert the field's `:field/value` \"backward\" to the `:field/initial-value`.
  This is a relative of [[snapshot-value]] which advances the field's value
  forward."
  [field]
  (assoc field :field/value (:field/initial-value field)))

(defn snapshot-value
  "Advance the field's `:field/initial-value` \"forward\" to the current
  `:field/value`. This is a relative of [[reset-value]] which reverts the
  field's value backward. To also reset the value tracking, see
  [[discard-value-history]], or for both, use [[snapshot]]."
  [field]
  (assoc field :field/initial-value (:field/value field)))

(defn init
  "Initialize the field, at the given `path`, with the provided `value` (default
  `nil`), and configured to track changes with the `tracker` (default
  `tracker/default-tracker`)."
  ([path] (init path nil))
  ([path value] (init path value tracker/default-tracker))
  ([path value tracker]
   (-> {:field/path    path
        :field/value   value
        :field/tracker tracker}
       (snapshot-value)
       (discard-history))))

(defn snapshot
  "Snapshot the field's value with [[snapshot-value]] and reset its value
  history with [[discard-value-history]].

  This may be useful to record that a field has been saved externally, e.g. to
  indicate that it has been synchronized with a backend server.

  Unlike [[reset]], `snapshot` does not discard history about the field's focus
  state."
  [field]
  (-> field (snapshot-value) (discard-value-history)))

(defn reset
  "Abandon all changes to a `field`, including any accumulated state history
  established by its tracker."
  [field]
  (init (:field/path field) (:field/initial-value field) (:field/tracker field)))

(defn gain-focus
  "Track that the `field` has gained focus."
  [field]
  (tracker/track field :focus-gained))

(defn lose-focus
  "Track that the `field` has lost focus."
  [field]
  (tracker/track field :focus-lost))

(defn change
  "Change the `value` of the `field`, tracking that history."
  [field value]
  (-> field
      (assoc :field/value value)
      (tracker/track :changed)))
