package com.fgobot.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fgobot.data.database.dao.ServantDao
import com.fgobot.data.database.entities.ServantEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServantDaoTest {
    
    private lateinit var database: FGODatabase
    private lateinit var servantDao: ServantDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FGODatabase::class.java
        ).allowMainThreadQueries().build()
        
        servantDao = database.servantDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertAndGetServant() = runBlocking {
        // Given
        val servant = ServantEntity(
            id = 1,
            name = "Artoria Pendragon",
            className = "Saber",
            rarity = 5,
            cost = 16,
            atkBase = 1734,
            atkMax = 11221,
            hpBase = 1881,
            hpMax = 12825,
            attribute = "Earth",
            gender = "Female",
            traits = "[]",
            skills = "[]",
            noblePhantasm = "[]",
            cardHits = "{}",
            starAbsorb = 100,
            starGen = 10.0f,
            npCharge = "{}",
            deathRate = 21.0f,
            criticalWeight = 102
        )
        
        // When
        servantDao.insertServant(servant)
        val retrievedServant = servantDao.getServantById(1)
        
        // Then
        assertNotNull(retrievedServant)
        assertEquals(servant.name, retrievedServant?.name)
        assertEquals(servant.className, retrievedServant?.className)
        assertEquals(servant.rarity, retrievedServant?.rarity)
    }
    
    @Test
    fun getOwnedServants() = runBlocking {
        // Given
        val ownedServant = ServantEntity(
            id = 1,
            name = "Artoria",
            className = "Saber",
            rarity = 5,
            cost = 16,
            atkBase = 1734,
            atkMax = 11221,
            hpBase = 1881,
            hpMax = 12825,
            attribute = "Earth",
            gender = "Female",
            traits = "[]",
            skills = "[]",
            noblePhantasm = "[]",
            cardHits = "{}",
            starAbsorb = 100,
            starGen = 10.0f,
            npCharge = "{}",
            deathRate = 21.0f,
            criticalWeight = 102,
            owned = true
        )
        
        val notOwnedServant = ownedServant.copy(id = 2, name = "Gilgamesh", owned = false)
        
        // When
        servantDao.insertServant(ownedServant)
        servantDao.insertServant(notOwnedServant)
        val ownedServants = servantDao.getOwnedServants().first()
        
        // Then
        assertEquals(1, ownedServants.size)
        assertEquals("Artoria", ownedServants[0].name)
        assertTrue(ownedServants[0].owned)
    }
    
    @Test
    fun updateServantOwnership() = runBlocking {
        // Given
        val servant = ServantEntity(
            id = 1,
            name = "Artoria",
            className = "Saber",
            rarity = 5,
            cost = 16,
            atkBase = 1734,
            atkMax = 11221,
            hpBase = 1881,
            hpMax = 12825,
            attribute = "Earth",
            gender = "Female",
            traits = "[]",
            skills = "[]",
            noblePhantasm = "[]",
            cardHits = "{}",
            starAbsorb = 100,
            starGen = 10.0f,
            npCharge = "{}",
            deathRate = 21.0f,
            criticalWeight = 102,
            owned = false
        )
        
        // When
        servantDao.insertServant(servant)
        servantDao.updateServantOwnership(1, true)
        val updatedServant = servantDao.getServantById(1)
        
        // Then
        assertNotNull(updatedServant)
        assertTrue(updatedServant!!.owned)
    }
} 