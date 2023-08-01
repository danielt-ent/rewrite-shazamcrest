/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.dtem;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.openrewrite.Tree.randomId;

public class NoShazamcrest extends Recipe {
    private static final MethodMatcher SAME_BEAN_AS = new MethodMatcher("com.shazam.shazamcrest.matcher.Matchers sameBeanAs(java.lang.Object)");
    private static final MethodMatcher STRING_EMPTY = new MethodMatcher("java.langString isEmpty()");
    private static final MethodMatcher ANY_ASSERT_THAT = new MethodMatcher("*..* assertThat(..)");
    private static final MethodMatcher NEW_ARRAY_LIST_ITERABLE = new MethodMatcher("com.google.common.collect.Lists newArrayList(java.lang.Iterable)");
    private static final MethodMatcher NEW_ARRAY_LIST_CAPACITY = new MethodMatcher("com.google.common.collect.Lists newArrayListWithCapacity(int)");

    @Override
    public String getDisplayName() {
        //language=markdown
        return "Use `new ArrayList<>()` instead of Guava";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Prefer the Java standard library over third-party usage of Guava in simple cases like this.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
            // Any change to the AST made by the preconditions check will lead to the visitor returned by Recipe
            // .getVisitor() being applied
            // No changes made by the preconditions check will be kept
            Preconditions.or(new UsesMethod<>(SAME_BEAN_AS),
                new UsesMethod<>(NEW_ARRAY_LIST_ITERABLE),
                new UsesMethod<>(NEW_ARRAY_LIST_CAPACITY)),
            // To avoid stale state persisting between cycles, getVisitor() should always return a new instance of
            // its visitor
            new JavaVisitor<ExecutionContext>() {
                private final JavaTemplate assertThat = JavaTemplate.builder("assertThat(#{any()})")
                        .staticImports("org.assertj.core.api.Assertions.assertThat")
                        .build();
                private final JavaTemplate assertThatContinued = JavaTemplate.builder("isEqualTo(#{any()})")
                    .imports("org.assertj.core.api.AbstractObjectAssert")
                    .build();

                private final JavaTemplate newArrayListIterable =
                    JavaTemplate.builder("new ArrayList<>(#{any(java.util.Collection)})")
                    .imports("java.util.ArrayList")
                    .build();

                private final JavaTemplate newArrayListCapacity =
                    JavaTemplate.builder("new ArrayList<>(#{any(int)})")
                    .imports("java.util.ArrayList")
                    .build();

                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                    if (STRING_EMPTY.matches(method)) {
                        System.out.println("method = " + method + ", executionContext = " + executionContext);
                        JavaType.Method methodType = method.getMethodType();
                        System.out.print(methodType);
                    } else if (ANY_ASSERT_THAT.matches(method)) {
                        maybeRemoveImport("com.shazam.shazamcrest.matcher.Matchers.sameBeanAs");
                        maybeRemoveImport("com.shazam.shazamcrest.MatcherAssert.assertThat");
                        maybeRemoveImport("org.hamcrest.MatcherAssert.assertThat");
                        maybeRemoveImport("org.junit.Assert.assertThat");
                        JavaType.ShallowClass classType = JavaType.ShallowClass.build("org.assertj.core.api.Assertions");
                        AtomicReference<J.MethodInvocation> sameBeanInvocation = findSameBeanInvocation(method, getCursor());
                        if (sameBeanInvocation.get() != null) {
                            maybeAddImport("org.assertj.core.api.Assertions", "assertThat", false);
                            maybeAddImport("org.assertj.core.api.Assertions", false);
//                            J.MethodInvocation newMI = assertThat.apply(getCursor(), method.getCoordinates().replace(),
//                                    method.getArguments().get(0));
//                            J.MethodInvocation continued = assertThatContinued.apply(updateCursor(method), method.getCoordinates().replace(),
//                                    sameBeanInvocation.get().getArguments().get(0));
//                            long flags = method.getMethodType().getFlagsBitMap();
//                            java.util.List<JavaType> javaTypes = new java.util.ArrayList<>();
//                            javaTypes.add(JavaType.buildType("java.lang.Object"));
//                            List<String> strings = new java.util.ArrayList<>();
//                            strings.add("actual");
                            JavaTemplate template = JavaTemplate.builder("Assertions.assertThat(#{any()})")
                                    .contextSensitive()
                                    .imports("org.assertj.core.api.Assertions")
                                    .build();
                            J.MethodInvocation apply = template.apply(updateCursor(method), method.getCoordinates().replace(), method.getArguments().get(0));
                            System.out.println("apply = " + apply);

                            return apply;

//                            newMI = newMI.withMethodType(new JavaType.Method(null, flags, classType, "assertThat", null, strings, javaTypes, null, null))
//                            ;
//                            JavaType.FullyQualified aoClassType = JavaType.ShallowClass.build("org.assertj.core.api.AbstractObjectAssert");
//                            continued = continued.withMethodType(new JavaType.Method(null, flags, aoClassType, "isEqualTo", null, strings, javaTypes, null, null))
//                                    .withSelect(newMI);
//
////                            new JLeftPadded<>(method.getSelect().getPrefix(), newMI, Markers.EMPTY);
//                            return continued;
//                            newMI = newMI.withSelect(
//                                    new J.Identifier(randomId(),
//                                            method.getSelect() == null ?
//                                                    Space.EMPTY :
//                                                    method.getSelect().getPrefix(),
//                                            Markers.EMPTY,
//                                            classType.getClassName(),
//                                            classType,
//                                            null
//                                    )
//                            );
//                            return newMI;
                        }
                    } else if (NEW_ARRAY_LIST_ITERABLE.matches(method)) {
                        maybeRemoveImport("com.google.common.collect.Lists");
                        maybeAddImport("java.util.ArrayList");
                        return newArrayListIterable.apply(getCursor(), method.getCoordinates().replace(),
                                method.getArguments().get(0));
                    } else if (NEW_ARRAY_LIST_CAPACITY.matches(method)) {
                        maybeRemoveImport("com.google.common.collect.Lists");
                        maybeAddImport("java.util.ArrayList");
                        return newArrayListCapacity.apply(getCursor(), method.getCoordinates().replace(),
                                method.getArguments().get(0));
                    }
                    return super.visitMethodInvocation(method, executionContext);
                }

            }
        );
    }
    @NotNull
    private AtomicReference<J.MethodInvocation> findSameBeanInvocation(J.MethodInvocation method, Cursor cursor) {
        return new JavaVisitor<AtomicReference<J.MethodInvocation>>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, AtomicReference<J.MethodInvocation> ctx) {
                if (ctx.get() != null)
                    return method;
                if (SAME_BEAN_AS.matches(method)) {
                    ctx.set(method);
                    return method;
                }
                return super.visitMethodInvocation(method, ctx);
            }
        }.reduce(method, new AtomicReference<>(), cursor);
    }

}
