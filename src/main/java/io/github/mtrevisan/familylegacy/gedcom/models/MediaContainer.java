/**
 * Copyright (c) 2020 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.gedcom.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public abstract class MediaContainer extends NoteContainer{

	private List<MediaRef> mediaRefs;
	private List<Media> media;


	/**
	 * @param gedcom	Gedcom
	 * @return	Inline media as well as referenced media
	 */
	public List<Media> getAllMedia(final Gedcom gedcom){
		final List<MediaRef> mediaRefs = getMediaRefs();
		final List<Media> media = new ArrayList<>(mediaRefs.size());
		for(final MediaRef mediaRef : mediaRefs){
			final Media m = mediaRef.getMedia(gedcom);
			if(m != null)
				media.add(m);
		}
		media.addAll(getMedia());
		return media;
	}

	List<MediaRef> getMediaRefs(){
		return mediaRefs != null? mediaRefs: Collections.emptyList();
	}

	public void setMediaRefs(final List<MediaRef> mediaRefs){
		this.mediaRefs = mediaRefs;
	}

	public void addMediaRef(final MediaRef mediaRef){
		if(mediaRefs == null)
			mediaRefs = new ArrayList<>(1);

		mediaRefs.add(mediaRef);
	}

	List<Media> getMedia(){
		return (media != null? media: Collections.emptyList());
	}

	public void setMedia(final List<Media> media){
		this.media = media;
	}

	public void addMedia(final Media mediaObject){
		if(media == null)
			media = new ArrayList<>(1);

		media.add(mediaObject);
	}

	//FIXME
//	public void visitContainedObjects(final Visitor visitor){
//		for(final MediaRef mediaRef : getMediaRefs())
//			mediaRef.accept(visitor);
//		for(final Media m : getMedia())
//			m.accept(visitor);
//
//		super.visitContainedObjects(visitor);
//	}

}
