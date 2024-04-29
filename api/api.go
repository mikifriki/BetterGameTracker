package api

import (
	"gametracker/data"
	"gametracker/db"
	"gametracker/util"
	"net/http"

	"github.com/gin-gonic/gin"
)

// Returns all current entries from db.json
func GetGames(c *gin.Context) {
	games := db.ReadGameEntry("testData/db.json")
	c.IndentedJSON(http.StatusOK, games)
}

// Creates new Game entry in the db.json
func CreateNew(c *gin.Context) {
	games := db.ReadGameEntry("testData/db.json")
	var newGame data.GameEntry

	err := c.BindJSON(&newGame)
	if err != nil {
		return
	}

	// Check if game exists
	if !util.GameExists(games, newGame) {
		games = append(games, newGame)
		db.CreateNewFile(games, "testData/"+"db"+".json")
		db.CheckAndCreateDir(newGame.GameTitle)
		c.IndentedJSON(http.StatusOK, games)
		return
	}
	c.String(http.StatusBadRequest, "Duplicate Entry")
}

// Update game entry in db.json
func UpdateGame(c *gin.Context) {
	games := db.ReadGameEntry("testData/db.json")
	var newGame data.GameEntry

	err := c.BindJSON(&newGame)
	if err != nil {
		return
	}

	// If entry exists then update the details.
	for index, element := range games {
		if element.GameTitle == newGame.GameTitle {
			games[index].Details = newGame.Details
		}
	}

	// Write to new file
	db.CreateNewFile(games, "testData/"+"db"+".json")
	c.IndentedJSON(http.StatusOK, newGame)
}

// Creates new play entry in the game directory.
func AddPlayEntry(c *gin.Context) {
	var newEntry data.PlayEntry
	err := c.BindJSON(&newEntry)
	if err != nil {
		return
	}

	allEntries := db.ReadPlayEntry("testData/" + newEntry.GameTitle + "details.json")
	// Check if game exists
	if !util.EntryExists(allEntries, newEntry) {
		allEntries = append(allEntries, newEntry)
		db.CreateNewFile(allEntries, "testData/"+newEntry.GameTitle+"details.json")
		c.IndentedJSON(http.StatusOK, allEntries)
		return
	}
	c.String(http.StatusBadRequest, "Duplicate Entry")

	c.IndentedJSON(http.StatusOK, allEntries)
}
