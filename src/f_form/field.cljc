(ns f-form.field
  "Conceptually, a `field` corresponds to a single HTML `<input>`, `<select>`, or
  `<textarea>`.

  A field has a `:field/path`, which uniquely identifies the field in a form. It
  is a sequence of keys as defined by [[clojure.core/assoc-in]].

  It has a `:field/value`, which can be any value: a primitive string, number or
  boolean, or a more elaborate map or set. The library provides a few tools for
  translating between the comparatively simple HTML tags and these more complex
  values, but often users of the library will build their own translators.

  It has a `:field/initial-value`, the value of the field before any changes.

  And finally, it has a `:field/tracker`. The tracker defines which other state
  and history attributes will be stored on the field. See
  [[f-form.field-tracker]] for more information about these
  attributes and how they are set."
  (:require [f-form.field-tracker :as tracker]))

(defn snapshot
  "Record that a field has been saved externally, e.g. to indicate that a field
  has been synchronized with a backend server. This is a relative of [[reset]]:
  reset reverts the `:field/value` \"backward\" to the `:field/initial-value`,
  but this advances the `:field/initial-value` \"forward\" to the current
  `:field/value`. Unlike [[reset]], does not discard history about the field's
  focus state."
  [field]
  (-> field
      (assoc :field/initial-value (:field/value field))
      (tracker/track :snapshot)))

(defn init
  "Initialize the field, at the given `path`, with the provided `value`, and
  configured to track changes with the `tracker`."
  ([path] (init path nil))
  ([path value] (init path value tracker/default-tracker))
  ([path value tracker]
   (-> {:field/path    path
        :field/value   value
        :field/tracker tracker}
       (tracker/track :initialized)
       (snapshot))))

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
