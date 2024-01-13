# Needle

![Badge](https://travis-ci.com/hyperpape/needle.svg?branch=main)
![Badge](https://www.repostatus.org/badges/latest/wip.svg)

String searching/matching algorithms in Java, including multi-string
matching and searching, and regular expressions.

The regular expression compiles regular expressions to Deterministic 
Finite Automata (DFA)FAs (meaning that the regular expressions are 
[non-backtracking](https://swtch.com/~rsc/regexp/regexp1.html)), and spends extra effort at compile-time to
give better runtime matching performance. Those compile-time efforts
currently take a few forms:

  1. Bytecode compilation: creating a specialized class for an
  individual regex that specializes the code to simulate an automaton,
  reducing interpretation overhead.

  2. Regular expressions with a literal prefix (e.g. `http://.*`) have
     specialized matching that seeks for a possible prefix using an
     explicit for loop (skipping the automaton code).

  3. The automaton can look ahead for necessary characters later in
     the stream. Upon seeing `"the "` when matching `"the [Cc]rown"`, 
     the automaton will check whether `n` is found 5 characters ahead, 
     before moving to the next state. If the look-ahead fails, it will 
     skip ahead and restart.

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

At build time, we can create a classfile and write it to the filesystem:

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

### Building

The compiler requires Java 11. Builds with maven. The generated
classes should work with Java 8.
