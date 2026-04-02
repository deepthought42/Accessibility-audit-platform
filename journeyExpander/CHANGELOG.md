## [Unreleased]

### Tests

* add comprehensive AuditController unit tests covering all receiveMessage branches, shouldBeExpanded, and existsInJourney
* add ApplicationTest for Spring Boot entry point
* add mockito-inline and byte-buddy dependencies for static method mocking
* add spring-cloud-gcp-pubsub test dependency for PubSub class loading
* achieve 90%+ code coverage across the codebase

### Documentation

* update README with architecture diagram, testing section, CI/CD, and Docker instructions
* update CONTRIBUTING with development setup, testing guidelines, and PR workflow

## [1.0.5](https://github.com/deepthought42/journeyExpander/compare/v1.0.4...v1.0.5) (2026-02-21)


### Bug Fixes

* address review feedback for payload validation and expansion flow ([a867712](https://github.com/deepthought42/journeyExpander/commit/a8677128d2ce1dc003b328d291ef97c8ad3abf60))
* harden merged controller null-safety checks ([9062c42](https://github.com/deepthought42/journeyExpander/commit/9062c42e386d0bf0e15b52d73d9ebd8ca8cb14fe))
* resolve merge null-comparison regressions in controller ([2e4483b](https://github.com/deepthought42/journeyExpander/commit/2e4483bde5ffebeae2c509f326205c9b663a874b))
* resolve merge regressions in controller and bootstrap ([b491cba](https://github.com/deepthought42/journeyExpander/commit/b491cbaff79bb8953426a7f67f0dd6dabc9a382b))
* sanitize branch name before docker image tagging ([ee0ba42](https://github.com/deepthought42/journeyExpander/commit/ee0ba42bf52948f9394a84aff5b36f045e364396))
* unblock docker image build pipeline ([dac8bca](https://github.com/deepthought42/journeyExpander/commit/dac8bcad7d80193389984697ee7a06f964b11349))

## [1.0.4](https://github.com/deepthought42/journeyExpander/compare/v1.0.3...v1.0.4) (2025-07-21)


### Bug Fixes

* upgrade core to 0.3.21 ([335cec0](https://github.com/deepthought42/journeyExpander/commit/335cec004b93f72d95948e57b39306dd6572435a))

## [1.0.3](https://github.com/deepthought42/journeyExpander/compare/v1.0.2...v1.0.3) (2025-07-21)


### Bug Fixes

* basedire for core jar ([dfd58d2](https://github.com/deepthought42/journeyExpander/commit/dfd58d26827fef9375d10026aacdceb42223ef0d))
* branch name master to main ([25b2b2d](https://github.com/deepthought42/journeyExpander/commit/25b2b2d7a1aa214cd89daa559a3f7c48c6e5a087))
* ci step to download core ([0e1f5b3](https://github.com/deepthought42/journeyExpander/commit/0e1f5b3608150252d23d5461834a145d57038b2b))
* core upgrade to 0.1.8 ([de0abca](https://github.com/deepthought42/journeyExpander/commit/de0abca0b508714f543429b33b0fcd62c53d1390))
* refactored services, models, and repos ([2946949](https://github.com/deepthought42/journeyExpander/commit/2946949a8b041cfc57bd15c6be4e900ccb927a0e))
* Release ci ([9b7f3e3](https://github.com/deepthought42/journeyExpander/commit/9b7f3e38ce6d8d2208def9a0e99a62e9cfb2742b))
* removed utils and updated core download jar version ([f9beb7a](https://github.com/deepthought42/journeyExpander/commit/f9beb7a5859d47a8bb882577e766cd672aa30585))
* test pipeline bumped to 21 ([52b253c](https://github.com/deepthought42/journeyExpander/commit/52b253c2c808b51601f1833ce93be7debdcc1fff))
* update core version ([3c1c4a7](https://github.com/deepthought42/journeyExpander/commit/3c1c4a792502c0b42d8b7ff17ee53256ec09c285))
* upgrade core to 0.3.20 ([975f6ee](https://github.com/deepthought42/journeyExpander/commit/975f6ee3fc7ce7b4da50347d4f4a91954fee1875))
* upgraded dockerfile and release pipeline. updated jdk to 21 ([eb91451](https://github.com/deepthought42/journeyExpander/commit/eb91451d6ab2b9675026c35ec21e655170f06543))

## [1.0.2](https://github.com/deepthought42/journeyExpander/compare/v1.0.1...v1.0.2) (2025-03-27)


### Bug Fixes

* renamed jar file executed in Dockerfile ([856f712](https://github.com/deepthought42/journeyExpander/commit/856f7129dce8d9bf5a54786acd1ad9c32e76728a))

## [1.0.1](https://github.com/deepthought42/journeyExpander/compare/v1.0.0...v1.0.1) (2025-03-27)


### Bug Fixes

* corrected docker build names and tags ([34f3c8f](https://github.com/deepthought42/journeyExpander/commit/34f3c8f52b88688da82dfdc0281743258e6438ed))

# 1.0.0 (2025-03-27)


### Bug Fixes

* renamed repo and project names ([f96fef8](https://github.com/deepthought42/journeyExpander/commit/f96fef80b208262a80a2e4b5672e9c6fa93b037e))
* updated dockerfile ([5d8cf9c](https://github.com/deepthought42/journeyExpander/commit/5d8cf9c54dcb62f4a19b97cbd33e879ea980aa5d))


### Features

* added semantic versioning and github action workflow ([4b45319](https://github.com/deepthought42/journeyExpander/commit/4b45319d2bcf9306a71ff7f4b731836258f419b8))
