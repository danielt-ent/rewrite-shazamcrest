package me.dtem;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NoShazamcrestTest implements RewriteTest {
    Object foo;

    //Note, you can define defaults for the RecipeSpec and these defaults will be used for all tests.
    //In this case, the recipe and the parser are common. See below, on how the defaults can be overridden
    //per test.
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoShazamcrest())
            .parser(JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(true)
                .classpath("shazamcrest"));
    }

    @Test
    void replaceWithNewArrayList() {
        rewriteRun(
            //There is an overloaded version or rewriteRun that allows the RecipeSpec to be customized specifically
            //for a given test. In this case, the parser for this test is configured to not log compilation warnings.
            spec -> spec
                .parser(JavaParser.fromJavaVersion()
                    .logCompilationWarningsAndErrors(false)
                    .classpath("hamcrest", "assertj-core", "shazamcrest")),
            //language=java
            java("""
                    import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
                    import static org.hamcrest.MatcherAssert.assertThat;
                    import static org.hamcrest.core.Is.is;
                    
                    class Test {
                        public void someTest() {
                            assertThat(new Object(), is(sameBeanAs(new Object())));
                        }
                    }
                    """,
                """
                    import org.assertj.core.api.Assertions;
                    
                    import static org.assertj.core.api.Assertions.assertThat;
                    import static org.hamcrest.core.Is.is;
                    
                    class Test {
                        public void someTest() {
                            Assertions.assertThat(new Object());
                        }
                    }
                    """
            )
        );
    }

    @Test
    void replaceWithNewArrayListIterable() {
        rewriteRun(
            //language=java
            java("""
                    import com.google.common.collect.*;
                    
                    import java.util.Collections;
                    import java.util.List;
                    
                    class Test {
                        List<Integer> l = Collections.emptyList();
                        List<Integer> cardinalsWorldSeries = Lists.newArrayList(l);
                    }
                    """,
                """
                    import java.util.ArrayList;
                    import java.util.Collections;
                    import java.util.List;
                    
                    class Test {
                        List<Integer> l = Collections.emptyList();
                        List<Integer> cardinalsWorldSeries = new ArrayList<>(l);
                    }
                    """
            )
        );
    }

    @Test
    void replaceWithNewArrayListWithCapacity() {
        rewriteRun(
            //language=java
            java("""
                import com.google.common.collect.*;
                
                import java.util.ArrayList;
                import java.util.List;
                
                class Test {
                    List<Integer> cardinalsWorldSeries = Lists.newArrayListWithCapacity(2);
                }
                """,
            """
                import java.util.ArrayList;
                import java.util.List;
                
                class Test {
                    List<Integer> cardinalsWorldSeries = new ArrayList<>(2);
                }
                """)
        );
    }
}
