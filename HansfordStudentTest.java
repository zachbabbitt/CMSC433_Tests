package cmsc433.p1.tests;

import static org.junit.Assert.*;

import java.util.List;

import cmsc433.p1.AuctionServer;
import cmsc433.p1.Item;

public class HansfordStudentTest {

	// Tests correctness of the AuctionServer class, in a single-threaded environment.
	@org.junit.Test
	public void singleThreadedTest() throws InterruptedException {
		AuctionServer auctionServer = AuctionServer.getInstance();
		
		// No items have yet been added - check results of methods.
		assertEquals(-1, auctionServer.itemPrice(0));
		assertEquals(true, auctionServer.itemUnbid(-1));
		assertEquals(0, auctionServer.getItems().size());
		assertEquals(3, auctionServer.checkBidStatus("Johnny", 0));	
		assertEquals(0, auctionServer.soldItemsCount());
		assertEquals(0, auctionServer.revenue());
		
		// An item is successfully added. Check results of methods.
		int toyCarId = auctionServer.submitItem("Bobby", "ToyCar", 4, 100);
		assertFalse(-1 == toyCarId);
		assertEquals(1, auctionServer.getItems().size());
		assertEquals(2, auctionServer.checkBidStatus("Johnny", toyCarId));
		assertEquals(4, auctionServer.itemPrice(toyCarId));
		assertEquals(true, auctionServer.itemUnbid(toyCarId));
		
		// The ToyCar auction expired without bids. Check the results.
		Thread.sleep(200);
		assertEquals(1, auctionServer.getItems().size());
		assertEquals(4, auctionServer.itemPrice(toyCarId));
		assertEquals(true, auctionServer.itemUnbid(toyCarId));
		
		// Bid status has now been checked, so item should be removed from the list.
		assertEquals(3, auctionServer.checkBidStatus("Johnny", toyCarId));
		assertEquals(4, auctionServer.itemPrice(toyCarId));
		assertEquals(0, auctionServer.getItems().size());
		
		// Revenue and soldItemsCount should not have changed.
		assertEquals(0, auctionServer.revenue());
		assertEquals(0, auctionServer.soldItemsCount());
		
		// A bid on the ToyCar should fail
		assertFalse(auctionServer.submitBid("Johnny", toyCarId, 4));

		// Now add another item to the auction
		int toyTruckId = auctionServer.submitItem("Bobby", "ToyTruck", 5, 100);
		assertFalse(-1 == toyTruckId);
		assertEquals(1, auctionServer.getItems().size());
		assertEquals("ToyTruck", auctionServer.getItems().get(0).name());
		assertEquals(2, auctionServer.checkBidStatus("Johnny", toyTruckId));
		assertEquals(5, auctionServer.itemPrice(toyTruckId));
		assertEquals(true, auctionServer.itemUnbid(toyTruckId));
		
		// A bid below the opening price should be denied
		assertFalse(auctionServer.submitBid("Ellie", toyTruckId, 4));
		
		// Someone places a bid on the ToyTruck, at the opening price.
		assertTrue(auctionServer.submitBid("Johnny", toyTruckId, 5));
		assertEquals(false, auctionServer.itemUnbid(toyTruckId));
		assertEquals(5, auctionServer.itemPrice(toyTruckId));
		
		// If Johnny tries to bid again, his bid should be denied
		assertFalse(auctionServer.submitBid("Johnny", toyTruckId, 6));
		
		// Ellie cannot bid $5, since Johnny already did, so her request should be denied
		assertFalse(auctionServer.submitBid("Ellie", toyTruckId, 5));
		
		// However, Ellie can bid $6, to outbid Johnny.
		assertTrue(auctionServer.submitBid("Ellie", toyTruckId, 6));
		assertEquals(6, auctionServer.itemPrice(toyTruckId));
		
		// Bidding should still be open (or your processor is slower than a turtle).
		assertEquals(2, auctionServer.checkBidStatus("Ellie", toyTruckId));
		assertTrue(auctionServer.getItems().get(0).biddingOpen());
		
		// Wait for auction to end.
		Thread.sleep(200);
		assertFalse(auctionServer.getItems().get(0).biddingOpen());
		
		// Until checkBidStatus is called, the auctionServer should not reflect the bidding is over.
		assertEquals(1, auctionServer.getItems().size());
		assertEquals(0, auctionServer.soldItemsCount());
		assertEquals(0, auctionServer.revenue());
		
		// Ellie has won the auction, so if she checks the bid status, it should return success.
		assertEquals(1, auctionServer.checkBidStatus("Ellie", toyTruckId));
		
		// The auctionServer should now reflect that the ToyTruck was sold to Ellie for $6.
		assertEquals(0, auctionServer.getItems().size());
		assertEquals(1, auctionServer.soldItemsCount());
		assertEquals(6, auctionServer.revenue());
		
		// Johnny lost the auction, so if he checks the bid status, it should return failure.
		assertEquals(3, auctionServer.checkBidStatus("Johnny", toyTruckId));
		
		// Timmy didn't bid, so if he checks the bid status, it should also return failure
		assertEquals(3, auctionServer.checkBidStatus("Timmy", toyTruckId));
		
		// Calls to itemPrice and itemUnbid should still report correctly
		assertEquals(6, auctionServer.itemPrice(toyTruckId));
		assertEquals(false, auctionServer.itemUnbid(toyTruckId));
		
		// The following test will cause an exception if this statement is false
		assertTrue(AuctionServer.serverCapacity >= AuctionServer.maxSellerItems);
		
		// Test the limit for a single seller.
		int [] itemIds = new int [AuctionServer.maxSellerItems];
		for (int i = 0; i < AuctionServer.maxSellerItems; i++) {
			itemIds[i] = auctionServer.submitItem("Bobby", "TestItem" + i, 1, 1000);
			assertFalse(-1 == itemIds[i]);
		}
		
		// If Bobby attempts to add another item, it should fail
		assertEquals(-1, auctionServer.submitItem("Bobby", "OneItemTooMany", 1, 1000));
		
		// Test the state of the server
		assertEquals(AuctionServer.maxSellerItems, auctionServer.getItems().size());
		for (int i = 0; i < itemIds.length; i++) {
			assertEquals(true, auctionServer.itemUnbid(itemIds[i]));
			assertEquals(1, auctionServer.itemPrice(itemIds[i]));
			assertEquals(2, auctionServer.checkBidStatus("Johnny", itemIds[i]));
		}
		
		// The following test will cause an exception this statement is false.
		assertTrue(AuctionServer.maxSellerItems > AuctionServer.maxBidCount);
		
		// Test the limit for a single bidder
		for (int i = 0; i < AuctionServer.maxBidCount; i++) {
			assertTrue(auctionServer.submitBid("Johnny", itemIds[i], 1));
		}
		assertFalse(auctionServer.submitBid("Johnny", itemIds[AuctionServer.maxBidCount], 1));
		
		// However, if Johnny is outbid on an item, he should be able to place another bid
		assertTrue(auctionServer.submitBid("Timmy", itemIds[0], 2));
		assertTrue(auctionServer.submitBid("Johnny", itemIds[AuctionServer.maxBidCount], 1));
		
		// Wait for bidding to close and cleanup the items
		Thread.sleep(1100);
		for (int i = 0; i < itemIds.length; i++) {
			assertEquals(3, auctionServer.checkBidStatus("Ellie", itemIds[i]));
		}
		assertEquals(1 + 1 + AuctionServer.maxBidCount, auctionServer.soldItemsCount());
		assertEquals(6 + 2 + AuctionServer.maxBidCount, auctionServer.revenue());
		assertEquals(0, auctionServer.getItems().size());
		
		// Bobby should now be permitted to list another item
		int toyBoatId = auctionServer.submitItem("Bobby", "ToyBoat", 7, 100);
		assertFalse(-1 == toyBoatId);
		// Johnny should also be permitted to place a bid again
		assertTrue(auctionServer.submitBid("Johnny", toyBoatId, 7));
		
		// Wait for the ToyBoat to be sold to Johnny
		Thread.sleep(200);
		assertEquals(1, auctionServer.checkBidStatus("Johnny", toyBoatId));
		assertEquals(2 + 1 + AuctionServer.maxBidCount, auctionServer.soldItemsCount());
		assertEquals(8 + 7 + AuctionServer.maxBidCount, auctionServer.revenue());
		
		// Max out the serverCapacity
		for (int i = 0; i < AuctionServer.serverCapacity; i++) {
			assertFalse(-1 == auctionServer.submitItem("TestSeller" + i, "ABrandNewCar!", 30000, 1000));
		}
		
		// This item should be rejected, since the server is at capacity.
		assertTrue(-1 == auctionServer.submitItem("Bobby", "ToyHelicopter", 8, 100));
		
		// Test that messing with the returned list from getItems() does not mess up the server
		List<Item> items = auctionServer.getItems();
		assertEquals(AuctionServer.serverCapacity, items.size());
		items.remove(0);
		assertEquals(AuctionServer.serverCapacity, auctionServer.getItems().size());
		assertTrue(-1 == auctionServer.submitItem("Bobby", "ToyHelicopter", 8, 100));
	}

}
