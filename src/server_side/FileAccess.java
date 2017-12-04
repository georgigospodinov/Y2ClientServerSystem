package server_side;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

/**
 * This class has information about the directories the {@link Server} can access.
 * Its methods are static and it should not be instantiated.
 *
 * @author 150009974
 * @version 1.2
 */
class FileAccess {

    /**
     * The set of directories that the {@link Server} can access.
     */
    private static HashSet<File> directories = new HashSet<>();

    /**
     * Loads the set of directories that the {@link Server} can access
     * in the {@link FileAccess#directories} hash set.
     *
     * @param dirs an array of strings which are the names of accessible directories
     */
    static void loadDirectories(String[] dirs) {

        File file;
        for ( String dir : dirs ) {
            file = new File(dir);
            if (file.isDirectory())
                directories.add(file);
        }

    }

    /**
     * Finds an returns the canonical path to a given file within a given directory.
     * If the file is not in this directory (or a subdirectory within), the method returns null.
     *
     * @param directoryToSearch the directory in which the file is searched for
     * @param fileToFind the file to be found
     * @return the canonical path to the file OR null
     */
    private static String findPath(File directoryToSearch, String fileToFind) {

        String path = null;
        try {
            path = directoryToSearch.getCanonicalPath();
        }
        catch (IOException e) {
            System.out.println("Could not get canonical path of " + directoryToSearch);
        }
        File maybe = new File(path + File.separator + fileToFind);
        if ( maybe.exists())
            return path;

        File[] subDirs = directoryToSearch.listFiles();
        if ( subDirs == null )
            return null;

        for ( File f : subDirs ) {
            if ( f.isDirectory() ) {
                String pathFound = findPath(f, fileToFind);
                if ( pathFound != null ) {
                    return pathFound;
                }
            }
        }

        return null;

    }

    /**
     * Finds an returns the canonical path to a given file.
     * Makes repeated calls to {@link FileAccess#findPath(File, String)}
     * with all elements of {@link FileAccess#directories}.
     * If the file is not found, the method returns null.
     * If multiple such files exits, the first one is returned.
     *
     * @param fileToFind the file to be found
     * @return the canonical path to the file OR null
     */
    static String getFullPath(String fileToFind) {

        for ( File dir : directories ) {
            String pathFound = findPath(dir, fileToFind);
            if ( pathFound != null ) {
                return pathFound+File.separator+fileToFind;
            }
        }

        return null;

    }
}
