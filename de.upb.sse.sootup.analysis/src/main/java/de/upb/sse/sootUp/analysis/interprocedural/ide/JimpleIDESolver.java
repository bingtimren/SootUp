package de.upb.sse.sootup.analysis.interprocedural.ide;

/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 2022 Kadiray Karakaya and others
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

import de.upb.sse.sootup.core.jimple.common.stmt.Stmt;
import de.upb.sse.sootup.core.model.SootMethod;
import heros.IDETabulationProblem;
import heros.InterproceduralCFG;
import heros.solver.IDESolver;

public class JimpleIDESolver<D, V, I extends InterproceduralCFG<Stmt, SootMethod>>
    extends IDESolver<Stmt, D, SootMethod, V, I> {

  public JimpleIDESolver(IDETabulationProblem<Stmt, D, SootMethod, V, I> problem) {
    super(problem);
  }

  @Override
  public void solve() {
    super.solve();
  }
}
