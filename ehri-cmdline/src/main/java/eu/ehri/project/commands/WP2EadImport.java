/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.commands;

import eu.ehri.project.importers.Wp2EadHandler;
import eu.ehri.project.importers.Wp2EadImporter;

/**
 *
 * @author linda
 */
public class WP2EadImport extends EadImport{
    final static String NAME = "terezin-ead-import";
	
	public WP2EadImport() {
		super(Wp2EadHandler.class, Wp2EadImporter.class);
	}
}
