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
package ail.syntax;

import java.util.Set;

import ail.tracing.explanations.PredicateDescriptions;

/**
 * An action description with explicit terminating conditions and a duration:
 *
 *     {pre} a {post} : (success, failure, abort)[duration]
 *
 * Following Dennis and Fisher (2014) an executing action terminates when the agent believes its
 * success, failure or abort condition.  If no success condition is given the post-condition is used.
 * If no failure condition is given, an action that the environment reports as finished without its
 * success condition being believed has failed.  If no abort condition is given the action is aborted
 * only when it has run for longer than its duration (and only in an environment that keeps time).
 *
 * The threshold is the number of failures, recorded in the agent's action log, after which the
 * action is considered deprecated and a new description is learned for it.
 *
 * The original pre- and post-conditions are kept when a new description is learned.  They record
 * what the programmer intended the action to achieve and are used as the goal when re-planning.
 *
 * @author Peter Stringer
 */
public class DurativeAction extends Capability {
	/** Number of failures after which an action is deprecated if no threshold is given. */
	public static final int DEFAULT_THRESHOLD = 1;

	GLogicalFormula success;
	GLogicalFormula failure;
	GLogicalFormula abort;
	int duration = 0;
	int threshold = DEFAULT_THRESHOLD;

	GLogicalFormula originalPre;
	GLogicalFormula originalPost;
	boolean learned = false;

	/**
	 * Constructor for an action description with default terminating conditions.
	 * @param pr the pre-condition.
	 * @param c the action.
	 * @param pt the post-condition.
	 */
	public DurativeAction(GLogicalFormula pr, Predicate c, GLogicalFormula pt) {
		super(pr, c, pt);
		originalPre = pr;
		originalPost = pt;
	}

	/**
	 * Constructor.
	 * @param pr the pre-condition.
	 * @param c the action.
	 * @param pt the post-condition.
	 * @param s the success condition, or null to use the post-condition.
	 * @param f the failure condition, or null.
	 * @param ab the abort condition, or null.
	 * @param d the expected duration of the action, 0 if the action has no time limit.
	 * @param t the failure threshold.
	 */
	public DurativeAction(GLogicalFormula pr, Predicate c, GLogicalFormula pt, GLogicalFormula s, GLogicalFormula f, GLogicalFormula ab, int d, int t) {
		this(pr, c, pt);
		success = s;
		failure = f;
		abort = ab;
		duration = d;
		threshold = t;
	}

	/**
	 * The condition under which the action has succeeded.
	 * @return the success condition.
	 */
	public GLogicalFormula getSuccess() {
		if (success == null) {
			return getPost();
		}
		return success;
	}

	/**
	 * The condition under which an executing action has failed.
	 * @return the failure condition, or null if none was given.
	 */
	public GLogicalFormula getFailure() {
		return failure;
	}

	/**
	 * The condition under which an executing action should be aborted.
	 * @return the abort condition, or null if none was given.
	 */
	public GLogicalFormula getAbort() {
		return abort;
	}

	public int getDuration() {
		return duration;
	}

	public int getThreshold() {
		return threshold;
	}

	/**
	 * The pre-condition the action was originally given by the programmer.
	 * @return
	 */
	public GLogicalFormula getOriginalPre() {
		return originalPre;
	}

	/**
	 * The post-condition the action was originally given by the programmer.  This is
	 * what the action is intended to achieve.
	 * @return
	 */
	public GLogicalFormula getOriginalPost() {
		return originalPost;
	}

	/**
	 * Has this description been learned from the action log?
	 * @return
	 */
	public boolean isLearned() {
		return learned;
	}

	/**
	 * Create a new description of this action with a learned post-condition.  The learned post-condition
	 * becomes the new success condition (Stringer, Section 6.3.1) while the failure and abort conditions,
	 * duration, threshold and the originally intended description are kept.
	 * @param newpost the learned post-condition.
	 * @return the new action description.
	 */
	public DurativeAction withLearnedPost(GLogicalFormula newpost) {
		DurativeAction d = new DurativeAction(pre.clone(), cap.clone(), newpost, null, cloneOrNull(failure), cloneOrNull(abort), duration, threshold);
		d.originalPre = originalPre.clone();
		d.originalPost = originalPost.clone();
		d.learned = true;
		return d;
	}

	private static GLogicalFormula cloneOrNull(GLogicalFormula f) {
		if (f == null) {
			return null;
		}
		return f.clone();
	}

	/*
	 * (non-Javadoc)
	 * @see ail.syntax.Capability#clone()
	 */
	@Override
	public DurativeAction clone() {
		DurativeAction d = new DurativeAction(pre.clone(), cap.clone(), post.clone(), cloneOrNull(success), cloneOrNull(failure), cloneOrNull(abort), duration, threshold);
		d.originalPre = originalPre.clone();
		d.originalPost = originalPost.clone();
		d.learned = learned;
		d.setID(getID());
		return d;
	}

	/*
	 * (non-Javadoc)
	 * @see ail.syntax.Capability#getVarNames()
	 */
	@Override
	public Set<String> getVarNames() {
		Set<String> varnames = super.getVarNames();
		addVarNames(varnames, success);
		addVarNames(varnames, failure);
		addVarNames(varnames, abort);
		addVarNames(varnames, originalPre);
		addVarNames(varnames, originalPost);
		return varnames;
	}

	private static void addVarNames(Set<String> varnames, GLogicalFormula f) {
		if (f != null) {
			varnames.addAll(f.getVarNames());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see ail.syntax.Capability#renameVar(java.lang.String, java.lang.String)
	 */
	@Override
	public void renameVar(String oldname, String newname) {
		super.renameVar(oldname, newname);
		if (success != null) success.renameVar(oldname, newname);
		if (failure != null) failure.renameVar(oldname, newname);
		if (abort != null) abort.renameVar(oldname, newname);
		// Until the description is cloned the original conditions are the same objects as pre and post.
		if (originalPre != pre) originalPre.renameVar(oldname, newname);
		if (originalPost != post) originalPost.renameVar(oldname, newname);
	}

	/*
	 * (non-Javadoc)
	 * @see ail.syntax.Capability#apply(ail.syntax.Unifier)
	 */
	@Override
	public boolean apply(Unifier theta) {
		boolean changed = super.apply(theta);
		if (success != null) changed = success.apply(theta) || changed;
		if (failure != null) changed = failure.apply(theta) || changed;
		if (abort != null) changed = abort.apply(theta) || changed;
		if (originalPre != pre) changed = originalPre.apply(theta) || changed;
		if (originalPost != post) changed = originalPost.apply(theta) || changed;
		return changed;
	}

	/*
	 * (non-Javadoc)
	 * @see ail.syntax.Capability#toString()
	 */
	@Override
	public String toString() {
		return super.toString() + annotationString();
	}

	/*
	 * (non-Javadoc)
	 * @see ail.syntax.Capability#toString(ail.tracing.explanations.PredicateDescriptions)
	 */
	@Override
	public String toString(PredicateDescriptions descriptions) {
		return super.toString(descriptions) + annotationString();
	}

	private String annotationString() {
		StringBuilder s = new StringBuilder();
		if (success != null) s.append(" success {").append(success).append("}");
		if (failure != null) s.append(" failure {").append(failure).append("}");
		if (abort != null) s.append(" abort {").append(abort).append("}");
		if (duration > 0) s.append(" duration ").append(duration);
		if (threshold != DEFAULT_THRESHOLD) s.append(" threshold ").append(threshold);
		if (s.length() == 0) {
			return "";
		}
		return " [" + s.toString().trim() + "]";
	}
}
