/**
 *
 */
package de.cinovo.surveyplatform.util;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class TreeUtil {
	
	public static class TreeNode<T> {
		
		private T data;
		private TreeNode<T> parent;
		private Set<TreeNode<T>> children;
		private long refID;
		
		
		public TreeNode(final T data) {
			this.data = data;
		}
		
		// public void setParent(final Object data) {
		// parent = new TreeNode(data);
		// }
		
		public void setParent(final TreeNode<T> parentNode) {
			this.parent = parentNode;
			this.parent.addChild(this);
		}
		
		public void addChild(final TreeNode<T> childNode) {
			if (this.children == null) {
				this.children = new LinkedHashSet<TreeUtil.TreeNode<T>>();
			}
			this.children.add(childNode);
			childNode.parent = this;
		}
		
		public void addChild(final T data) {
			this.addChild(new TreeNode<T>(data));
		}
		
		public Set<TreeNode<T>> getChildren() {
			return this.children;
		}
		
		public Set<T> getChildrenData() {
			HashSet<T> childs = new LinkedHashSet<T>();
			if (this.children != null) {
				for (TreeNode<T> childNode : this.children) {
					childs.add(childNode.getData());
				}
			}
			return childs;
		}
		
		public T getData() {
			return this.data;
		}
		
		public long getRefID() {
			return this.refID;
		}
		
		public void setRefID(final long refID) {
			this.refID = refID;
		}
	}
	
	
	public static <T> TreeNode<T> convertTableToTree(final Set<T> entityList, final String idField, final String parentField) {
		TreeNode<T> rootNode = new TreeNode<T>(null);
		
		// build flat nodes map remembering the IDs, semantic is: Map<ID,
		// TreeNode>
		Map<Object, TreeNode<T>> nodesMap = new LinkedHashMap<Object, TreeUtil.TreeNode<T>>();
		for (T entity : entityList) {
			try {
				Method method = entity.getClass().getMethod(idField);
				Object id = method.invoke(entity);
				nodesMap.put(id, new TreeNode<T>(entity));
			} catch (Exception e) {
				Logger.errUnexpected(e, null);
			}
		}
		// link the nodes together
		for (Entry<Object, TreeNode<T>> entry : nodesMap.entrySet()) {
			try {
				TreeNode<T> treeNode = entry.getValue();
				T data = treeNode.getData();
				Method parentGetter = data.getClass().getMethod(parentField);
				Object parent = parentGetter.invoke(data);
				if (parent == null) {
					rootNode.addChild(treeNode);
				} else {
					Method method = parent.getClass().getMethod(idField);
					Object parentId = method.invoke(parent);
					
					TreeNode<T> parentNode = nodesMap.get(parentId);
					parentNode.addChild(treeNode);
				}
			} catch (Exception e) {
				Logger.errUnexpected(e, null);
			}
		}
		return rootNode;
	}
	
	// public class TestEntity {
	// private long id = UUID.randomUUID().getMostSignificantBits();
	// private TestEntity parent;
	// private String name;
	//
	//
	// public TestEntity(final String name) {
	// this.name = name;
	// }
	//
	// /**
	// * @return the id
	// */
	// public long getId() {
	// return id;
	// }
	//
	// /**
	// * @param id the id to set
	// */
	// public void setId(final long id) {
	// this.id = id;
	// }
	//
	// /**
	// * @return the parent
	// */
	// public TestEntity getParent() {
	// return parent;
	// }
	//
	// /**
	// * @param parent the parent to set
	// */
	// public void setParent(final TestEntity parent) {
	// this.parent = parent;
	// }
	//
	// }
	//
	//
	// public static void main(final String[] args) {
	//
	// TreeUtil util = new TreeUtil();
	//
	// TestEntity t1 = util.new TestEntity("1");
	// TestEntity t2 = util.new TestEntity("2");
	// TestEntity t3 = util.new TestEntity("3");
	// TestEntity t4 = util.new TestEntity("4");
	// TestEntity t5 = util.new TestEntity("5");
	// TestEntity t6 = util.new TestEntity("6");
	// TestEntity t7 = util.new TestEntity("7");
	//
	// t1.setParent(null);
	// t2.setParent(t1);
	// t3.setParent(null);
	// t4.setParent(t1);
	// t5.setParent(t2);
	// t6.setParent(t2);
	// t7.setParent(t1);
	//
	// Set<TestEntity> testSet = new HashSet<TreeUtil.TestEntity>();
	// testSet.add(t1);
	// testSet.add(t2);
	// testSet.add(t3);
	// testSet.add(t4);
	// testSet.add(t5);
	// testSet.add(t6);
	// testSet.add(t7);
	// TreeNode<TestEntity> root = TreeUtil.convertTableToTree(testSet, "getId",
	// "getParent");
	//
	// printTree(root, 0);
	//
	// }
	//
	// /**
	// * @param root
	// */
	// private static void printTree(final TreeNode<TestEntity> node, final int
	// depth) {
	// for (int i = 0; i < depth; i++) {
	// System.out.print("-");
	// }
	// if (node.data == null) {
	// System.out.print("x");
	// } else {
	// System.out.print(node.data.name);
	// }
	// System.out.println();
	// if (node.children != null) {
	// for (TreeNode<TestEntity> child : node.children) {
	// printTree(child, depth + 2);
	// }
	// }
	// }
}
