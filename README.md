[![codecov](https://codecov.io/gh/JuulLabs/stropping/branch/master/graph/badge.svg)](https://codecov.io/gh/JuulLabs/stropping)

# Deprecation

Stropping isn't necessary in the world of [Hilt](https://dagger.dev/hilt/).
If you need the ability to uninstall individual classes from your tests, see [Pommel](https://github.com/JuulLabs/pommel) instead, an annotation processor which generates single-definition modules.

# Stropping

**Stropping** performs reflection on Dagger.
It injects itself into your `Application`, replacing the dagger component (and the `androidInjector` if `DaggerAndroid` is used).
The replacement component is mutable at runtime, which allows per-test overrides of single fields.

This is useful when performing tests which have need to communicate with external services, such as when using [mockwebserver].
The traditional approach for installing this in your application, as illustrated by [OkCupid's blogpost], is to create a subclass of your `Application` and launch it using a custom `AndroidJUnitRunner`.
Using this method allows a much more opt-in behavior on a per-test basis, without making _any_ changes to application code (such as marking a class as `open`).

## Mocking API

To make a change in your dependency graph, call `Replacements.of<YourAppComponent>`.
Inside that block, you have access to the `overwrite` and `addIntoMap` functions, which modify the dependency injection graph.
Additionally, there are convenience functions for use with `MockK` mocks, `overwriteWithMockK` and `addMockKIntoMap`.

These replacements are meant to take place before your activity is created.
As such, the easiest way of creating your activity is to use an `ActivityScenario` inside of your test, instead of creating a `@Rule` for activity creation.

Overall, tests end up looking something like this:

```kotlin
    @Test
    fun exampleTest() {
        Replacements.of<YourComponent> {
            overwriteWithMockK<YourInterface> {
                every { yourMethod() } returns "your value"
            }
        }
        val scenario = ActivityScenario.launch(YourActivity::class.java)
        // Do test stuff
        scenario.close()
    }
```

# Setup

## Prerequisites

**Stropping**'s mocking abilities are implemented by reflecting over your `Application` instance.
The implementation assumes that your application extends from `DaggerApplication`, and that your activities/fragments are injected by calls to `AndroidInjection.inject` (automatic for `DaggerActivity` and the like).

**Note:** support for replacing the application component as a field has been added, but is not yet robust.
However, it supports one of the most common/easy uses:

```kotlin
class MyApplication {
    val component: MyComponent by lazy {
        // Instantiate `DaggerMyComponent` here
    }
}
```

## Gradle

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.juul.stropping/stropping/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.juul.stropping/stropping)

In your project-level `build.gradle`:

```gradle
allprojects {
    repositories {
        jcenter() // or mavenCentral()
    }
}
```

Then, in your app `build.gradle`:

```gradle
dependencies {
    androidTestImplementation "com.juul.stropping:stropping:$version"
}
```

# Known Weaknesses

* It's slow. Mitigated by the fact this is intended for tests only, so it never effects users.
* Mocking takes place _after_ `Application.onCreate`, which has a few side-effects:
    * `@Singleton` instances injected after the replacement will not match with those injected into the `Application`
    * Modifications of global state, such as static variables used inside `@Provides` functions, can be triggered twice.
* Young/unimplemented features:
    * No scopes
    * No `@ElementsIntoSet`
    * No empty `@Multibinds`

# License

```
Copyright 2020 JUUL Labs, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

[mockwebserver]: https://github.com/square/okhttp/tree/master/mockwebserver
[OkCupid's Blogpost]: https://tech.okcupid.com/ui-tests-with-mockwebserver/
