package com.universeprojects.miniup.server.commands;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.universeprojects.cacheddatastore.CachedDatastoreService;
import com.universeprojects.cacheddatastore.CachedEntity;
import com.universeprojects.miniup.CommonChecks;
import com.universeprojects.miniup.server.GameUtils;
import com.universeprojects.miniup.server.ODPDBAccess;
import com.universeprojects.miniup.server.commands.framework.UserErrorMessage;

public class CommandItemsStackSplit extends CommandItemsBase {

	public CommandItemsStackSplit(ODPDBAccess db, HttpServletRequest request, HttpServletResponse response) {
		super(db, request, response);
	}

	@Override
	protected void processBatchItems(Map<String, String> parameters, ODPDBAccess db, CachedDatastoreService ds,
			CachedEntity character, List<CachedEntity> batchItems) throws UserErrorMessage {
		Long splitItemQuantity;
		if (batchItems.size() > 1) {
			throw new UserErrorMessage("You can only split one item at a time.");
		}
		ds.beginTransaction();
		try {
			CachedEntity splitItem = batchItems.get(0);
			
			if (CommonChecks.isItemCustom(splitItem)) 
				throw new UserErrorMessage("You cannot split a custom item.");
			
			splitItem = db.getDB().refetch(splitItem);
			
			if (GameUtils.equals(splitItem.getProperty("containerKey"), character.getKey()) == false) {
				throw new UserErrorMessage("Item does not belong to character.");
			}
			if (!splitItem.hasProperty("quantity")) {
				throw new UserErrorMessage("You can only split stackable items.");
			}
			splitItemQuantity = (Long) splitItem.getProperty("quantity");
			if (splitItemQuantity==null){
				throw new UserErrorMessage("You can only split stackable items.");
			}
			if (splitItemQuantity < 2) {
				throw new UserErrorMessage("You can only split stacks of two or more items.");
			}
			
			Long stackSize;
			try {
				stackSize = tryParseId(parameters, "stackSize");
			} catch (NumberFormatException e) {
				throw new UserErrorMessage("Please input an integer value.");
			}
			if (stackSize <= 0) {
				throw new UserErrorMessage("Please input a positive integer value.");
			}
			if (stackSize >= splitItemQuantity) {
				throw new UserErrorMessage(
						"Please input a quantity smaller than the size of the stack you are trying to split.");
			}
			long newStackSize = splitItemQuantity - stackSize;

			ds.preallocateIdsFor(splitItem.getKind(), 1);
			CachedEntity newItemStack = new CachedEntity(splitItem.getKind(), ds.getPreallocatedIdFor(splitItem.getKind()));
			CachedDatastoreService.copyFieldValues(splitItem, newItemStack);
			splitItem.setProperty("quantity", newStackSize);
			ds.put(splitItem);
			newItemStack.setProperty("quantity", stackSize);
			ds.put(newItemStack);
			
			ds.commit();
		} finally {
			ds.rollbackIfActive();
		}

		setJavascriptResponse(JavascriptResponse.ReloadPagePopup);
	}

}
