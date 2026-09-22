/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.sugiyama;

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Graph{

	public static class Node{
		private final String id;
		private final TreeNode treeNode;
		private final boolean dummy;
		private int layer = -1;
		private double x = 0.;
		private double y = 0.;
		private int width = 220;
		private int height = 90;

		private final List<Node> incoming = new ArrayList<>();
		private final List<Node> outgoing = new ArrayList<>();


		public Node(final String id, final TreeNode treeNode, final boolean dummy){
			this.id = id;
			this.treeNode = treeNode;
			this.dummy = dummy;
		}

		public String getId(){
			return id;
		}

		public TreeNode getTreeNode(){
			return treeNode;
		}

		public boolean isDummy(){
			return dummy;
		}

		public int getLayer(){
			return layer;
		}

		public void setLayer(final int layer){
			this.layer = layer;
		}

		public double getX(){
			return x;
		}

		public void setX(final double x){
			this.x = x;
		}

		public double getY(){
			return y;
		}

		public void setY(final double y){
			this.y = y;
		}

		public int getWidth(){
			return width;
		}

		public void setWidth(final int width){
			this.width = width;
		}

		public int getHeight(){
			return height;
		}

		public void setHeight(final int height){
			this.height = height;
		}

		public List<Node> getIncoming(){
			return incoming;
		}

		public List<Node> getOutgoing(){
			return outgoing;
		}

	}

	private final Map<String, Node> nodes = new HashMap<>();

	public Node addNode(final String id, final TreeNode treeNode, final boolean dummy){
		return nodes.computeIfAbsent(id, k -> new Node(k, treeNode, dummy));
	}

	public void addEdge(final String sourceId, final String targetId){
		final Node src = nodes.get(sourceId);
		final Node tgt = nodes.get(targetId);
		if(src != null && tgt != null && !src.outgoing.contains(tgt)){
			src.outgoing.add(tgt);
			tgt.incoming.add(src);
		}
	}

	public void removeEdge(final Node src, final Node tgt){
		if(src != null && tgt != null){
			src.outgoing.remove(tgt);
			tgt.incoming.remove(src);
		}
	}

	public Map<String, Node> getNodes(){
		return nodes;
	}

}
