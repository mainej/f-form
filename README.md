# f-form

Simple immutable form management for Clojure(Script).

## Installation

Install from Clojars:

[![Clojars Project](https://img.shields.io/clojars/v/com.github.mainej/f-form.svg)](https://clojars.org/com.github.mainej/f-form)

Install unreleased sha from Github (assuming Clojure CLI 1.10.3.933 or higher):

```clojure
# deps.edn
{:deps
  {com.github.mainej/f-form {:git/sha "<recent sha>"}}}
```

Updating? See the [CHANGELOG.md][changelog].

## Background

`f-form` aims to be a pared down functional Clojure version of [Final
Form][final-form], a JS library for managing form state.

The primary goal of a form, on the web or in a CLI, is to collect data from a
user. But data entry is only part of the story. Many UIs need to remember and
respond to the various ways that a user might interact with a form. As a form
designer, you may want to show a user which fields they have completed, are
currently working on, or still have to finish. You may want to warn them about
invalid data, but only after they have interacted with an invalid field. You may
want to prevent form submission until all errors are resolved, until certain
data is entered or changed, or while a previous submission is in flight. There's
a lot to keep track of besides the values that the user has entered.

At the same time, there are only a few actions a user can take. They can start a
form, switch between fields, enter data, and submit.

`f-form` provides a minimal set of tools to define forms and their fields and
track a user's interactions.

But most form libraries do this. What makes `f-form` unique is what it doesn't
do:

* **No state management.** The `f-form` suite of tools accepts and returns
  immutable forms that contain field values and a summarized history of a user's
  interactions with them. But that's not enough... an app has to store those
  forms and update them over time. `f-form` expects you to choose and use your
  own state manager. The ClojureScript community has a rich set of options for
  managing state. If you are using React, you may want to store state locally
  with reagent, or in a re-frame app-db. If you are using `f-form` in another
  context, the choice is up to you.
* **No HTML or CSS.** `f-form` makes no decisions about how DOM elements are
  generated or how to style them. Actually, it doesn't care whether you are
  working in the DOM, in a CLI, or elsewhere. Instead, it provides the data you
  need to make your own UI. It aims to do this efficiently by tracking only the
  interactions you care about.
* **No validation.** Though `f-form` expects that you will validate your forms,
  it does not require validation or any particular validation library, nor does
  it introduce its own validation syntax. Validation happens externally, not
  internally. `f-form` provides a sample, optional, set of tools for client-side
  validation with `vlad`, and contributions for other validation libraries are
  welcome, but these will always be opt-in.
* **No submission logic.** `f-form` helps you decide _whether_ to submit, but
  not _how_ to submit. The tradeoff is that you have to inform `f-form` when you
  are submitting.

## Learn more

* Run an example [reagent project that uses f-form][reagent-example].
* See the [API Docs][docs].
* Review the [code and tests][code].
* Interested in contributing? Review the [contributing guidelines][contrib].

## Alternatives

- [`luciodale/fork`][fork] is another good option. It does a lot of state
  management internally, which ties it to reagent or re-frame and hides some
  useful functionality.
- [`efraimmgon/reframe-forms`][reframe-forms], as the name suggests, is closely
  tied to re-frame. It generates the HTML and does its own state management.
- [`jkk/formative`][formative] is the maximal approach including validation,
  rendering, even server-side parsing.

## License

Copyright © 2021 Jacob Maine

Distributed under the MIT License.

[code]: https://github.com/mainej/f-form
[docs]: https://cljdoc.org/d/com.github.mainej/f-form
[contrib]: https://github.com/mainej/f-form/blob/main/CONTRIBUTING.md
[changelog]: https://github.com/mainej/f-form/blob/main/CHANGELOG.md
[reagent-example]: https://github.com/mainej/f-form/tree/main/examples/reagent

[final-form]: https://final-form.org/
[fork]: https://github.com/luciodale/fork
[reframe-forms]: https://github.com/efraimmgon/reframe-forms
[formative]: https://github.com/jkk/formative
