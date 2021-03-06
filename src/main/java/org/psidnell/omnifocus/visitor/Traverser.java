/*
 * Copyright 2015 Paul Sidnell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.psidnell.omnifocus.visitor;

import org.psidnell.omnifocus.model.Context;
import org.psidnell.omnifocus.model.Folder;
import org.psidnell.omnifocus.model.Node;
import org.psidnell.omnifocus.model.Project;
import org.psidnell.omnifocus.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author psidnell
 *
 *         An implementation of the visitor pattern.
 *
 *         Walks the node tree applying Visitor methods on descent, ascent and on all lists of sub
 *         nodes.
 *
 *         A descriptor controls which node types are visited.
 *
 */
public class Traverser {

    private static final Logger LOGGER = LoggerFactory.getLogger(Traverser.class);

    public static void traverse(Visitor visitor, Node node) {
        try {
            doTraverse(visitor, visitor.getWhat(), node);
        } catch (Exception e) {
            throw new TraversalException(e);
        }
    }

    private static void doTraverse(Visitor visitor, VisitorDescriptor what, Node node) throws Exception {
        LOGGER.debug("Traversing node: {}", node);
        try {
            switch (node.getType()) {
                case Folder.TYPE:
                    doTraverseFolder(visitor, what, (Folder) node);
                    break;
                case Project.TYPE:
                    doTraverseProject(visitor, what, (Project) node);
                    break;
                case Context.TYPE:
                    doTraverseContext(visitor, what, (Context) node);
                    break;
                case Task.TYPE:
                    doTraverseTask(visitor, what, (Task) node, true);
                    break;
                default:
                    break;
            }
        } catch (NodeTraversalAbortException e) {
            LOGGER.debug("Traversal aborted: {}", node);
        }
    }

    private static void doTraverseFolder(Visitor visitor, VisitorDescriptor what, Folder node) throws Exception {

        try {
            if (!what.getVisitFolders()) {
                return;
            }

            LOGGER.debug("Traversing folder: {}", node);

            visitor.enter(node);

            if (what.getFilterFolders()) {
                node.setFolders(visitor.filterFoldersDown(node.getFolders()));
            }

            if (what.getFilterProjects()) {
                node.setProjects(visitor.filterProjectsDown(node.getProjects()));
            }

            for (Folder child : node.getFolders()) {
                doTraverseFolder(visitor, what, child);
            }

            if (what.getVisitProjects()) {
                for (Project child : node.getProjects()) {
                    doTraverseProject(visitor, what, child);
                }
            }

            if (what.getFilterFolders()) {
                node.setFolders(visitor.filterFoldersUp(node.getFolders()));
            }

            if (what.getFilterProjects()) {
                node.setProjects(visitor.filterProjectsUp(node.getProjects()));
            }

            visitor.exit(node);
        } catch (NodeTraversalAbortException e) {
            LOGGER.debug("Traversal aborted: {}", node);
        }
    }

    private static void doTraverseTask(Visitor visitor, VisitorDescriptor what, Task node, boolean fromProject) throws Exception {
        try {
            if (!what.getVisitTasks()) {
                return;
            }

            LOGGER.debug("Traversing task: {}", node);

            visitor.enter(node);

            if (what.getFilterTasks()) {
                node.setTasks(visitor.filterTasksDown(node.getTasks()));
            }

            if (fromProject) {
                // Tasks are flat in the context hierarchy
                for (Task child : node.getTasks()) {
                    doTraverseTask(visitor, what, child, fromProject);
                }
            }

            if (what.getFilterTasks()) {
                node.setTasks(visitor.filterTasksUp(node.getTasks()));
            }

            visitor.exit(node);
        } catch (NodeTraversalAbortException e) {
            LOGGER.debug("Traversal aborted: {}", node);
        }
    }

    private static void doTraverseContext(Visitor visitor, VisitorDescriptor what, Context node) throws Exception {
        try {
            if (!what.getVisitContexts()) {
                return;
            }

            LOGGER.debug("Traversing context: {}", node);

            visitor.enter(node);

            if (what.getFilterTasks()) {
                node.setTasks(visitor.filterTasksDown(node.getTasks()));
            }

            if (what.getFilterContexts()) {
                node.setContexts(visitor.filterContextsDown(node.getContexts()));
            }

            if (what.getVisitTasks()) {
                for (Task child : node.getTasks()) {
                    doTraverseTask(visitor, what, child, false);
                }
            }

            for (Context child : node.getContexts()) {
                doTraverseContext(visitor, what, child);
            }

            if (what.getFilterTasks()) {
                node.setTasks(visitor.filterTasksUp(node.getTasks()));
            }

            if (what.getFilterContexts()) {
                node.setContexts(visitor.filterContextsUp(node.getContexts()));
            }
            visitor.exit(node);
        } catch (NodeTraversalAbortException e) {
            LOGGER.debug("Traversal aborted: {}", node);
        }
    }

    private static void doTraverseProject(Visitor visitor, VisitorDescriptor what, Project node) throws Exception {
        try {
            if (!what.getVisitProjects()) {
                return;
            }

            LOGGER.debug("Traversing project: {}", node);

            visitor.enter(node);

            if (what.getFilterTasks()) {
                node.setTasks(visitor.filterTasksDown(node.getTasks()));
            }

            if (what.getVisitTasks()) {
                for (Task child : node.getTasks()) {
                    doTraverseTask(visitor, what, child, true);
                }
            }

            if (what.getFilterTasks()) {
                node.setTasks(visitor.filterTasksUp(node.getTasks()));
            }

            visitor.exit(node);
        } catch (NodeTraversalAbortException e) {
            LOGGER.debug("Traversal aborted: {}", node);
        }
    }
}
