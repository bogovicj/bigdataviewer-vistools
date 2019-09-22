/*-
 * #%L
 * UI for BigDataViewer.
 * %%
 * Copyright (C) 2017 - 2018 Tim-Oliver Buchholz
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.util.uipanel;

import bdv.util.BdvSource;
import bdv.viewer.Source;
import bdv.viewer.state.SourceState;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * GroupProperties holds all information about a group added to the UI.
 *
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class GroupProperties {
	private String groupName;
	private Set<Source> sources;
	private boolean visibility;

	/**
	 * Information about a specific group.
	 *
	 * @param groupName name of the group
	 * @param source name of the source to add
	 * @param visibility of this group
	 */
	public GroupProperties(final String groupName, final Source source, final boolean visibility) {
		this.groupName = groupName;
		this.sources = new HashSet<>();
		this.sources.add(source);
		this.visibility = visibility;
	}

	/**
	 * Creates an empty group with no source.
	 *
	 * @param groupName name of the group
	 * @param visibility of the group
	 */
	public GroupProperties(final String groupName, final boolean visibility) {
		this.groupName = groupName;
		this.sources = new HashSet<>();
		this.visibility = visibility;
	}

	/**
	 *
	 * @return group name
	 */
	public String getGroupName() {
		return groupName;
	}

	/**
	 * Returns the set of sources assigned to this group.
	 *
	 * @return all sources in group
	 */
	public Set<Source> getSources() {
		return sources;
	}

	/**
	 * Add a source to this group.
	 *
	 * @param source the source to add
	 */
	public void addSource(final Source source) {
		sources.add(source);
	}

	/**
	 * Remove a source from this group.
	 *
	 * @param source the source to remove
	 */
	public void removeSource(final Source source) {
		sources.remove(source);
	}

	public boolean isVisible() {
		return visibility;
	}

	/**
	 * Set visibility.
	 *
	 * @param visibility of this group
	 */
	public void setVisible(final boolean visibility) {
		this.visibility = visibility;
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof GroupProperties) {
			GroupProperties g = (GroupProperties) obj;
			return g.getGroupName() == this.groupName;
		}
		return false;
	}
}
