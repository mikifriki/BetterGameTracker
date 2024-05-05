package api

import (
	"gametracker/data"
	"gametracker/db"
	"gametracker/util"
	"net/http"

	"github.com/gin-gonic/gin"
)

type JSONApi struct{}

// Returns all current entries from db.json
func (JSONApi) GetGames(c *gin.Context) {
	games, err := db.ReadGameEntries(util.GlobalConfig.MainJsonDbDirectory + "db.json")
	if err != nil {
		c.IndentedJSON(http.StatusBadRequest, "No games found")
		return
	}

	c.IndentedJSON(http.StatusOK, games)
}

// Creates new Game entry in the db.json
func (JSONApi) CreateNewGameEntry(c *gin.Context) {
	games, _ := db.ReadGameEntries(util.GlobalConfig.MainJsonDbDirectory + "db.json")
	var newGame data.GameEntry

	bindErr := c.BindJSON(&newGame)
	if bindErr != nil {
		c.IndentedJSON(http.StatusBadRequest, "Could not create JSON object")
		return
	}

	// Check if game exists
	if !util.GameExists(games, newGame) {
		*games = append(*games, newGame)
		db.CreateNewFile(*games, util.GlobalConfig.MainJsonDbDirectory+"db.json")
		db.CheckAndCreateDir(util.GlobalConfig.MainJsonDbDirectory + newGame.GameTitle)
		c.IndentedJSON(http.StatusOK, games)
		return
	}
	c.String(http.StatusBadRequest, "Duplicate Entry")
}

// Update game entry in db.json
func (JSONApi) UpdateGameEntry(c *gin.Context) {
	games, readErr := db.ReadGameEntries(util.GlobalConfig.MainJsonDbDirectory + "db.json")
	if readErr != nil {
		c.IndentedJSON(http.StatusBadRequest, "No Game Entries found")
		return
	}

	var newGame data.GameEntry
	updatedEntry := false
	err := c.BindJSON(&newGame)
	if err != nil {
		return
	}

	// If entry exists then update the details.
	for index, element := range *games {
		if element.GameTitle == newGame.GameTitle {
			(*games)[index].Details = newGame.Details
			updatedEntry = true
			break
		}
	}

	if !updatedEntry {
		c.IndentedJSON(http.StatusBadRequest, "Failed to update game entry")
		return
	}

	// Write to new file
	db.CreateNewFile(*games, util.GlobalConfig.MainJsonDbDirectory+"db.json")
	c.IndentedJSON(http.StatusOK, newGame)
}

// Creates new play entry in the game directory.
func (JSONApi) AddPlayEntry(c *gin.Context) {
	var newEntry data.PlayEntry
	err := c.BindJSON(&newEntry)
	if err != nil {
		c.IndentedJSON(http.StatusBadRequest, "Could not create JSON object")
		return
	}

	existingEntries, readErr := db.ReadPlayEntries(util.GlobalConfig.MainJsonDbDirectory + "db.json" + newEntry.GameTitle + "/details.json")

	// If error is nil then check for a duplicate entry
	if readErr == nil {
		for _, entry := range *existingEntries {
			if entry.Id == newEntry.Details.Id {
				c.IndentedJSON(http.StatusBadRequest, "Duplicate Detail Entry")
				return
			}
		}
	}

	*existingEntries = append(*existingEntries, newEntry.Details)
	db.CreateNewFile(*existingEntries, util.GlobalConfig.MainJsonDbDirectory+newEntry.GameTitle+"/details.json")
	c.IndentedJSON(http.StatusOK, existingEntries)
}

// Updates a play entry.
func (JSONApi) UpdatePlayEntry(c *gin.Context) {
	var existingEntry data.PlayEntry
	err := c.BindJSON(&existingEntry)
	if err != nil {
		c.IndentedJSON(http.StatusBadRequest, "Could not create JSON object")
		return
	}

	existingEntries, readErr := db.ReadPlayEntries(util.GlobalConfig.MainJsonDbDirectory + existingEntry.GameTitle + "/details.json")

	// If duplicate exists then update the details
	if readErr == nil {
		for index, entry := range *existingEntries {
			if (entry.Id == existingEntry.Details.Id) && entry != existingEntry.Details {
				// Update the array entry by index.
				(*existingEntries)[index] = existingEntry.Details
			}
		}
	} else {
		c.IndentedJSON(http.StatusBadRequest, "No play entries found")
		return
	}
	db.CreateNewFile(*existingEntries, util.GlobalConfig.MainJsonDbDirectory+existingEntry.GameTitle+"/details.json")
	c.IndentedJSON(http.StatusOK, existingEntries)
}
