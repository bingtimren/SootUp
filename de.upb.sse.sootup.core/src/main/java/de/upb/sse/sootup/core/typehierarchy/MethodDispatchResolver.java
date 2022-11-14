package de.upb.sse.sootup.core.typehierarchy;
/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 2019-2022 Christian Brüggemann, Jonas Klauke
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import com.google.common.collect.Sets;
import de.upb.sse.sootup.core.frontend.ResolveException;
import de.upb.sse.sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import de.upb.sse.sootup.core.model.Method;
import de.upb.sse.sootup.core.model.SootClass;
import de.upb.sse.sootup.core.model.SootMethod;
import de.upb.sse.sootup.core.signatures.MethodSignature;
import de.upb.sse.sootup.core.types.ClassType;
import de.upb.sse.sootup.core.views.View;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public final class MethodDispatchResolver {
  private MethodDispatchResolver() {}

  /**
   * Searches the view for classes that are subtypes of the class contained in the signature.
   * returns method signatures to all subtypes. Abstract methods are filtered the returned set can
   * contain signatures of not implemented methods.
   */
  @Nonnull
  public static Set<MethodSignature> resolveAllDispatches(
      View<? extends SootClass<?>> view, MethodSignature m) {
    TypeHierarchy hierarchy = view.getTypeHierarchy();

    return hierarchy.subtypesOf(m.getDeclClassType()).stream()
        .map(
            subtype ->
                view.getClass(subtype)
                    .orElseThrow(
                        () ->
                            new ResolveException(
                                "Could not resolve " + subtype + ", but found it in hierarchy.")))
        .filter(
            sootClass -> {
              SootMethod sootMethod = sootClass.getMethod(m.getSubSignature()).orElse(null);
              // method is not implemented or not abstract
              return sootMethod == null || !sootMethod.isAbstract();
            })
        .map(sootClass -> new MethodSignature(sootClass.getType(), m.getSubSignature()))
        .collect(Collectors.toSet());
  }

  /**
   * Searches the view for classes that implement or override the method <code>m</code> and returns
   * the set of method signatures that a method call could resolve to.
   */
  @Nonnull
  public static Set<MethodSignature> resolveAbstractDispatch(
      View<? extends SootClass<?>> view, MethodSignature m) {
    TypeHierarchy hierarchy = view.getTypeHierarchy();

    return hierarchy.subtypesOf(m.getDeclClassType()).stream()
        .map(
            subtype ->
                view.getClass(subtype)
                    .orElseThrow(
                        () ->
                            new ResolveException(
                                "Could not resolve " + subtype + ", but found it in hierarchy.")))
        .flatMap(abstractClass -> abstractClass.getMethods().stream())
        .filter(potentialTarget -> canDispatch(m, potentialTarget.getSignature(), hierarchy))
        .filter(method -> !method.isAbstract())
        .map(Method::getSignature)
        .collect(Collectors.toSet());
  }

  /**
   * Searches the view for classes that implement or override the method <code>m</code> and returns
   * the set of method signatures that a method call could resolve to within the given classes.
   */
  @Nonnull
  public static Set<MethodSignature> resolveAbstractDispatchInClasses(
      View<? extends SootClass<?>> view, MethodSignature m, Set<ClassType> classes) {
    TypeHierarchy hierarchy = view.getTypeHierarchy();

    return hierarchy.subtypesOf(m.getDeclClassType()).stream()
        .map(
            subtype ->
                view.getClass(subtype)
                    .orElseThrow(
                        () ->
                            new ResolveException(
                                "Could not resolve " + subtype + ", but found it in hierarchy.")))
        .filter(c -> classes.contains(c.getType()))
        .flatMap(abstractClass -> abstractClass.getMethods().stream())
        .filter(potentialTarget -> canDispatch(m, potentialTarget.getSignature(), hierarchy))
        .filter(method -> !method.isAbstract())
        .map(Method::getSignature)
        .collect(Collectors.toSet());
  }

  /**
   * Searches the view for classes that implement or override the method <code>m</code> and returns
   * the set of method signatures that a method call could resolve to within the given classes.
   *
   * @param filteredSignatures all resolvable method signatures that are not within the given
   *     classes are added to this set
   */
  @Nonnull
  public static Set<MethodSignature> resolveAbstractDispatchInClasses(
      View<? extends SootClass<?>> view,
      MethodSignature m,
      Set<ClassType> classes,
      Set<MethodSignature> filteredSignatures) {

    Set<MethodSignature> allSignatures = resolveAbstractDispatch(view, m);
    Set<MethodSignature> signatureInClasses = Sets.newHashSet();
    allSignatures.forEach(
        methodSignature -> {
          if (classes.contains(methodSignature.getDeclClassType())) {
            signatureInClasses.add(methodSignature);
          } else {
            filteredSignatures.add(methodSignature);
          }
        });

    return signatureInClasses;
  }

  /**
   * <b>Warning!</b> Assumes that for an abstract dispatch, <code>potentialTarget</code> is declared
   * in the same or a subtype of the declaring class of <code>called</code>.
   *
   * <p>For a concrete dispatch, assumes that <code>potentialTarget</code> is declared in the same
   * or a supertype of the declaring class of <code>called</code>.
   *
   * @return Whether name and parameters are equal and the return type of <code>potentialTarget
   *     </code> is compatible with the return type of <code>called</code>.
   */
  public static boolean canDispatch(
      MethodSignature called, MethodSignature potentialTarget, TypeHierarchy hierarchy) {
    return called.getName().equals(potentialTarget.getName())
        && called.getParameterTypes().equals(potentialTarget.getParameterTypes())
        && (called.getType().equals(potentialTarget.getType()) //return types are equal
            || hierarchy.isSubtype(called.getType(), potentialTarget.getType())); // covariant
  }

  /**
   * Searches for the signature of the method that is the concrete implementation of <code>m</code>.
   * This is done by checking each superclass and the class itself for whether it contains the
   * concrete implementation.
   */
  @Nonnull
  public static MethodSignature resolveConcreteDispatch(
      View<? extends SootClass<?>> view, MethodSignature m) {
    TypeHierarchy hierarchy = view.getTypeHierarchy();

    // search concrete method in the class itself and its super classes
    ArrayList<SootClass<?>> classesInHierachyOrder = new ArrayList<>();
    ClassType superClassType = m.getDeclClassType();
    do {
      ClassType finalSuperClassType = superClassType;
      SootClass<?> superClass =
          view.getClass(superClassType)
              .orElseThrow(
                  () ->
                      new ResolveException(
                          "Did not find class " + finalSuperClassType + " in View"));

      classesInHierachyOrder.add(superClass);

      SootMethod concreteMethod =
          superClass.getMethods().stream()
              .filter(potentialTarget -> canDispatch(m, potentialTarget.getSignature(), hierarchy))
              .findAny()
              .orElse(null);
      if (concreteMethod != null && !concreteMethod.isAbstract()) {
        // method found and it is not abstract
        return concreteMethod.getSignature();
      }

      superClassType = hierarchy.superClassOf(superClassType);
    } while (superClassType != null);

    // No super class contains the implemented method, search the concrete method in interfaces
    for (SootClass<?> clazz : classesInHierachyOrder) {
      SootMethod concreteDefaultMethod =
          clazz.getInterfaces().stream()
              .map(
                  interfaceType ->
                      view.getMethod(
                          view.getIdentifierFactory()
                              .getMethodSignature(interfaceType, m.getSubSignature())))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .filter(potentialTarget -> canDispatch(m, potentialTarget.getSignature(), hierarchy))
              .findAny()
              .orElse(null);

      if (concreteDefaultMethod != null && !concreteDefaultMethod.isAbstract()) {
        // method found and it is not abstract
        return concreteDefaultMethod.getSignature();
      }
    }

    throw new ResolveException("Could not find concrete method for " + m);
  }

  /**
   * Resolves the actual method called by the <code>specialInvokeExpr</code> that is contained by
   * <code>container</code>.
   */
  @Nonnull
  public static MethodSignature resolveSpecialDispatch(
      View<? extends SootClass<?>> view,
      JSpecialInvokeExpr specialInvokeExpr,
      MethodSignature container) {
    MethodSignature specialMethodSig = specialInvokeExpr.getMethodSignature();
    if (specialMethodSig.getSubSignature().getName().equals("<init>")) {
      return specialMethodSig;
    }

    SootMethod specialMethod =
        view.getClass(specialMethodSig.getDeclClassType())
            .flatMap(cl -> cl.getMethod(specialMethodSig.getSubSignature()))
            .orElse(null);
    if (specialMethod != null && specialMethod.isPrivate()) {
      return specialMethodSig;
    }

    if (view.getTypeHierarchy()
        .isSubtype(container.getDeclClassType(), specialMethodSig.getDeclClassType())) {
      return resolveConcreteDispatch(view, specialMethodSig);
    }

    return specialMethodSig;
  }
}
