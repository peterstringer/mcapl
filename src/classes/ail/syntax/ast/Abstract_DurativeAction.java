// ----------------------------------------------------------------------------
// Copyright (C) 2026 Peter Stringer, Louise A. Dennis
//
// This file is part of the Agent Infrastructure Layer (AIL)
//
// The AIL is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 3 of the License, or (at your option) any later version.
//
// The AIL is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with the AIL; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//
// To contact the authors:
// http://www.csc.liv.ac.uk/~lad
//
//----------------------------------------------------------------------------
package ail.syntax.ast;

import ail.syntax.DurativeAction;
import ail.syntax.GLogicalFormula;
import gov.nasa.jpf.vm.MJIEnv;

/**
 * Abstract version of {@link DurativeAction}: a capability with terminating conditions,
 * a duration and a failure threshold.
 *
 * @author Peter Stringer
 */
public class Abstract_DurativeAction extends Abstract_Capability {
	Abstract_GLogicalFormula success;
	Abstract_GLogicalFormula failure;
	Abstract_GLogicalFormula abort;
	int duration = 0;
	int threshold = DurativeAction.DEFAULT_THRESHOLD;

	public Abstract_DurativeAction(Abstract_Predicate p) {
		super(p);
	}

	public void setSuccess(Abstract_GLogicalFormula f) {
		success = f;
	}

	public void setFailure(Abstract_GLogicalFormula f) {
		failure = f;
	}

	public void setAbort(Abstract_GLogicalFormula f) {
		abort = f;
	}

	public void setDuration(int d) {
		duration = d;
	}

	public void setThreshold(int t) {
		threshold = t;
	}

	/*
	 * (non-Javadoc)
	 * @see ail.syntax.ast.Abstract_Capability#toMCAPL()
	 */
	@Override
	public DurativeAction toMCAPL() {
		return new DurativeAction(pre.toMCAPL(), cap.toMCAPL(), post.toMCAPL(), toMCAPL(success), toMCAPL(failure), toMCAPL(abort), duration, threshold);
	}

	private static GLogicalFormula toMCAPL(Abstract_GLogicalFormula f) {
		if (f == null) {
			return null;
		}
		return f.toMCAPL();
	}

	/*
	 * (non-Javadoc)
	 * @see ail.syntax.ast.Abstract_Capability#newJPFObject(gov.nasa.jpf.vm.MJIEnv)
	 */
	@Override
	public int newJPFObject(MJIEnv env) {
		int ref = env.newObject("ail.syntax.ast.Abstract_DurativeAction");
		env.setReferenceField(ref, "cap", cap.newJPFObject(env));
		env.setReferenceField(ref, "pre", pre.newJPFObject(env));
		env.setReferenceField(ref, "post", post.newJPFObject(env));
		if (success != null) {
			env.setReferenceField(ref, "success", success.newJPFObject(env));
		}
		if (failure != null) {
			env.setReferenceField(ref, "failure", failure.newJPFObject(env));
		}
		if (abort != null) {
			env.setReferenceField(ref, "abort", abort.newJPFObject(env));
		}
		env.setIntField(ref, "duration", duration);
		env.setIntField(ref, "threshold", threshold);
		return ref;
	}

	/*
	 * (non-Javadoc)
	 * @see ail.syntax.ast.Abstract_Capability#toString()
	 */
	@Override
	public String toString() {
		return super.toString() + " [success " + success + ", failure " + failure + ", abort " + abort + ", duration " + duration + ", threshold " + threshold + "]";
	}
}
