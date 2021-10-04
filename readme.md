# Needle

![Badge](https://travis-ci.com/hyperpape/needle.svg?branch=main)
![Badge](https://www.repostatus.org/badges/latest/wip.svg)

String searching/matching algorithms in Java, including multi-string
matching and searching, and regular expressions.

The regular expression engine is
[non-backtracking](https://swtch.com/~rsc/regexp/regexp1.html), and
spends extra effort at compile-time to give better runtime matching
performance. Those compile-time efforts currently take a few forms:

  1. Bytecode compilation: creating a specialized class for an
  individual regex that specializes the code to simulate an automaton,
  reducing interpretation overhead.

  2. Regular expressions with a literal prefix (e.g. `http://.*`) have
     specialized matching that seeks for a possible prefix using an
     explicit for loop (skipping the automaton code).

  3. The automaton can look ahead for necessary characters later in
     the stream. Upon seeing `h` in`http://.*`, the automaton will
     check whether `/` is found 6 characters ahead, before moving to
     the next state.

### Status

This project is pre version 0.1 and has no users as of yet.

### Usage

The library generates a pair of classes for each regex:a `Pattern`
(`com.justinblank.strings.Pattern`, not `java.util.regex.Pattern`),
and a `Matcher` (`com.justinblank.strings.Matcher`, not
`java.util.regex.Matcher`).

These classes can either be created at runtime, when including the
`needle-compiler` jar, or saved into a classfile which can be included
in an application's classpath with the `needle-types` library.

#### Runtime Creation

Each call to `DFACompiler.compile` will create a new class.

```java
static final Pattern URL_PATTERN = DFACompiler.compile("http://.+", "OverSimplifiedURLMatcher");
....
Matcher matcher = URL_PATTERN.matcher("http://www.google.com");
assertTrue(matcher.matches());
assertTrue(matcher.containedIn());
MatchResult matchResult = matcher.find();
assertTrue(matchResult.matched);
assertEquals(0, matchResult.start);
assertEquals(21, matchResult.end);
```

#### Precompilation

At build time:

```java
Precompile.precompile("http://.+", "OversimplifiedURLMatcher", somedirectory.getAbsolutePath());
```

At run-time:

```java
Pattern pattern = new OversimplifiedURLMatcher();
Matcher matcher = pattern.matcher("http://www.google.com");
assertTrue(matcher.matches());
```

See `Pattern` for the supported operations.

### Syntax

This library attempts to match the standard library syntax for all
supported operations. Capturing groups are not currently
supported. Backreferences are unlikely to ever be supported.

The following character classes are supported:

    \a, \d, \D, \e, \f, \h, \s, \S, \t, \w, \W, \x, \0, \., \\

### Unicode

The library supports searching against any string, however the needles
that we search for are currently limited to the BMP.

### Performance

I don't know the right way to summarize regular expression engine
performance. Each operation (match, containedIn, find) has different
performance characteristics, each of which may differ between short
and long target strings, matches and non-matches. Further, each
regular expression allows different optimizations. A regular
expression with a maximum possible length will fail to match a
too-long string in O(1) time. A regular expression with a prefix may
spend most of its time in a while loop looking for the first
character, which is much faster than having to dispatch to different
states of the automaton.

With those caveats, at commit
`3e7fbf70967c488f568ca76f20022ef8d0a9a227`, running the benchmarks
`SherlockBenchmark`, `EmailBenchmark`, `DigitBenchmark`,
`SherlockSingleShotBenchmark`, `DigitBenchmark`, `LargeRegexBenchmark`
from [the benchmarks](https://github.com/hyperpape/needle-benchmarks),
we have the following improvements compared to the java standard
library regex implementation:

| Metric         | Ratio (higher is better)  |
| -------------- |--------------------------:|
| Worst          | 1.729                     |
| Best           | 14.924                    |
| Geometric Mean | 4.261                     |

Any potential users are highly recommended to measure their own use
case. Examples of regexes that perform worse than the standard library
are welcome as bug reports.

Since each regular expression is compiled to a separate class, users
should avoid dynamically compiling large numbers of regexes. Note that
generated classes may sometimes be quite large (the current
compilation strategy is probably not suitable for very large regular
expressions).

### Building

The compiler requires Java 11. Builds with maven. The generated
classes should work with Java 8.
