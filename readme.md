# Needle

![Badge](https://www.repostatus.org/badges/latest/wip.svg)

Needle is a very fast regular expression library for the JVM. It works by compiling regular expressions to Deterministic
Finite Automata (DFA) (meaning that the regular expressions are
[non-backtracking](https://swtch.com/~rsc/regexp/regexp1.html)), and
then compiles them those to JVM ByteCode. Each regex becomes a
separate JVM class.

### Status

This project has no users as of yet, but should be usable. It would 
probably be advisable to precompile a static set of regexes, test 
them for your use cases, and verify that they perform well. 

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

At build time, we can create a classfile and write it to the filesystem:

```java
Precompile.precompile("http://.+", "OversimplifiedURLMatcher", somedirectory.getAbsolutePath());
```

At run-time, we can construct our class and build a matcher:

```java
Pattern pattern = new OversimplifiedURLMatcher();
Matcher matcher = pattern.matcher("http://www.google.com");
assertTrue(matcher.matches());
```

See `Pattern` for the supported operations.

### Compatibility and Syntax

This library attempts to match the standard library syntax for all
supported operations. For any regex, the results of using this library should
be the same as the standard library, or a `RegexSyntaxException` should be 
raised during parsing. Any other discrepancies should be reported as a bug.

Extracting matches for capturing groups are not currently supported. Lookahead
and lookbehind are not currently supported. Backreferences are unlikely to
ever be supported. 

Inline flags are not yet supported. 

The following character classes are supported:

    \a, \d, \D, \e, \f, \h, \H, \s, \S, \t, \w, \W, \x, \.

### Unicode

The library supports searching against any string, however the needles
that we search for are currently limited to the
[Basic Multilingual Plane](https://en.wikipedia.org/wiki/Plane_(Unicode)#Basic_Multilingual_Plane).
Regexes containing non-ascii characters are currently likely to be much
slower than ASCII regexes 
(see [Issue #16](https://github.com/hyperpape/needle/issues/16)). Testing of
non-ascii regexes and non-ascii matches is currently less comprehensive.

### Java versions

The compiler requires Java 11. Generated classes should work with Java 8.
