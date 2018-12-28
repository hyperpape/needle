Compiling string matching algorithms directly to Java bytecodes.

Includes an NFA/DFA interpreter, a regex -> NFA parser, an NFA -> DFA
compiler, and a DFA -> bytecode compiler. Unicode support is currently limited
to the BMP.

Very very early experiment.

### Performance

In principle generating classes specialized for a particular automaton
should be capable of outperforming Java regular expressions, which are
not specialized.

In practice, I have done no performance testing whatsoever. 

### Building

Requires Java 9.

    mvn compile