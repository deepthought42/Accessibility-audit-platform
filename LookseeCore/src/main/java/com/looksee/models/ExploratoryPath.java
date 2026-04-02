package com.looksee.models;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A set of vertex objects that form a sequential movement through a graph.
 *
 * invariant: pathKeys != null
 * invariant: pathObjects != null
 */
@Getter
@Setter
public class ExploratoryPath {
	private static Logger log = LoggerFactory.getLogger(ExploratoryPath.class);
	
	private List<String> pathKeys;
	private List<LookseeObject> pathObjects;
	private List<ActionOLD> possibleActions;

	/**
	 * Creates new instance of path setting it to the given path
	 * 
	 * @param path_keys list of keys to add to path
	 * @param current_path list of objects to add to path
	 * @param actions list of actions to add to path
	 * 
	 * precondition: path_keys != null
	 * precondition: current_path != null
	 * precondition: actions != null
	 */
	@Deprecated
	public ExploratoryPath(List<String> path_keys, List<LookseeObject> current_path, List<ActionOLD> actions){
		assert path_keys != null;
		assert current_path != null;
		assert actions != null;
		setPathKeys(path_keys);
		setPathObjects(current_path);
		setPossibleActions(actions);
	}

	/**
	 * Creates new instance of path setting it to the given path
	 * 
	 * @param path_keys list of keys to add to path
	 * @param current_path list of objects to add to path
	 * 
	 * precondition: path_keys != null
	 * precondition: current_path != null
	 */
	public ExploratoryPath(List<String> path_keys, List<LookseeObject> current_path){
		assert path_keys != null;
		assert current_path != null;
		setPathKeys(path_keys);
		setPathObjects(current_path);
	}
		
	/**
	 * Adds an object to path and sets whether or not this path spans multiple domains
	 * 
	 * @param obj object to add to path
	 * @return true if the object was added, false otherwise
	 * 
	 * precondition: obj != null
	 */
	public boolean add(LookseeObject obj){
		assert obj != null;
		return getPathObjects().add(obj);
	}
	
	/**
	 * Gets the size of the path
	 * 
	 * @return size of the path
	 */
	public int size(){
		return getPathObjects().size();
	}
	
	/**
	 * Gets the last Vertex in a path that is of type {@link PageState}
	 * 
	 * @return the last page in the path
	 */
	public PageState findLastPage(){
		List<LookseeObject> path_obj_list = getPathObjects();
		PageState page = null;

		for(LookseeObject obj : path_obj_list){
			if(obj instanceof PageState){
				page = (PageState)obj;
			}
		}

		return page;
	}
	
	/**
	 * Checks if the path has 2 pages that are the equal
	 * 
	 * @param path_objects list of {@link PageState}s to check
	 * @param page {@link PageState} to check for
	 * @param isSinglePage boolean indicating if the path is a single page
	 * 
	 * precondition: path_objects != null
	 * precondition: page != null
	 * 
	 * @return true if sequence appears more than once
	 */
	public static boolean hasCycle(List<PageState> path_objects, PageState page, boolean isSinglePage){
		assert path_objects != null;
		assert page != null;
	
		if(isSinglePage){
			return false;
		}
		
		//extract all pages
		//iterate through pages to see if any match
		log.info("Checking if exploratory path has a cycle");
		for(PageState path_obj : path_objects){
			if(path_obj.equals(page)){
				return true;
			}
		}

		return false;
	}
	
	/**
	 * Checks if the path has the same page more than once.
	 * 
	 * @param path list of {@link LookseeObject}s to check
	 * @return true if sequence appears more than once
	 *
	 * precondition: path != null
	 */
	public static boolean hasPageCycle(List<LookseeObject> path){
		assert path != null;
		for(int i = path.size()-1; i > 0; i--){
			for(int j = i-1; j>= 0; j--){
				if(path.get(i) instanceof PageState
						&& path.get(j) instanceof PageState
						&& path.get(i).equals(path.get(j)))
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Gets the first page in the path
	 *
	 * @return the first page in the path
	 */
	public PageState firstPage() {
		
		for(LookseeObject obj : getPathObjects()){
			if(obj instanceof PageState){
				return (PageState)obj;
			}
		}
		return null;
	}

	/**
	 * Adds an object to path
	 * 
	 * @param key key to add to path
	 * @return true if the key was added, false otherwise
	 * 
	 * precondition: key != null
	 */
	public boolean addToPathKeys(String key){
		assert key != null;
		return this.getPathKeys().add(key);
	}
	
	/**
	 * Adds an object to path
	 * 
	 * @param path_obj object to add to path
	 * 
	 * precondition: path_obj != null
	 */
	public void addPathObject(LookseeObject path_obj) {
		assert path_obj != null;
		this.pathObjects.add(path_obj);
	}
	
	/**
	 * Clone object
	 * 
	 * @param path {@link ExploratoryPath} to clone
	 * @return cloned {@link ExploratoryPath}
	 *
	 * precondition: path != null
	 */
	public static ExploratoryPath clone(ExploratoryPath path){
		assert path != null;
		List<LookseeObject> path_objects = new ArrayList<LookseeObject>(path.getPathObjects());
		List<String> path_keys = new ArrayList<String>(path.getPathKeys());
		
		return new ExploratoryPath(path_keys, path_objects);
	}
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o){
		if(o instanceof ExploratoryPath){
			ExploratoryPath path = (ExploratoryPath)o;
			List<LookseeObject> comparator_nodes = new ArrayList<LookseeObject>(path.getPathObjects());
			for(LookseeObject obj : getPathObjects()){
				int idx = 0;
				for(LookseeObject comparator_obj : path.getPathObjects()){
					if(comparator_obj.equals(obj)){
						comparator_nodes.remove(idx);
						break;
					}
					idx++;
				}
			}
			return comparator_nodes.isEmpty();
		}
		return false;
	}
}


