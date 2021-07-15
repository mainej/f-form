# Contributing

TODO: establish contributing guidelines here.

## Develop

* Run `bin/test` to run tests.
* Run `bin/coverage` for an updated code coverage report.
* Generate documentation
    * After cloning this project, run `git fetch origin gh-pages && git worktree add gh-pages gh-pages`.
    * Run `bin/doc` to generate documentation, accessible at `gh-pages/index.html`.
    * Run `bin/doc-release` to publish documentation to Github Pages.

## Deploy

### Prepare

1. Decide whether this will be a patch, minor, or major release.
    * Run `bin/preview-tag --patch` (or --minor, --major) to learn new version number.
2. Proactively update CHANGELOG.md for new version numnber.
3. Commit

### Release

Deploy to Clojars -- needs `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment
variables:

NOT: 
    $ envdir ../../env/clojars bin/clojars-release --patch # patch, minor, or major

But, temporarily (see bin/clojars-release for explanation):

    $ envdir ../../env/clojars bin/clojars-release --version X.Y.Z

The library will be deployed to [clojars.org][clojars].

Push to github, with tags:

    $ git push --follow-tags

[clojars]: https://clojars.org/com.github.mainej/f-form
