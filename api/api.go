package api

import (
	"gametracker/data"
	"gametracker/db"
	"gametracker/util"
	"net/http"

	"github.com/gin-gonic/gin"
)

// Returns all current entries
func GetGames(c *gin.Context) {
	games := db.ReadFile("testData/db.json")
	c.IndentedJSON(http.StatusOK, games)
}

// Creates new entry.
func CreateNew(c *gin.Context) {
	games := db.ReadFile("testData/db.json")
	var newGame data.GameEntry

	err := c.BindJSON(&newGame)
	if err != nil {
		return
	}

	// Create new object and fill initial.
	if len(games) == 0 {
		games = append(games, newGame)
		db.CreateNew(games)
		c.IndentedJSON(http.StatusOK, games)
		return
	}
	if !util.StructExists(games, newGame) {
		games = append(games, newGame)
		db.CreateNew(games)
		c.IndentedJSON(http.StatusOK, newGame)
		return
	}
	c.String(http.StatusBadRequest, "Bad value")
}

// Update game.
func UpdateGame(c *gin.Context) {
	games := db.ReadFile("testData/db.json")
	var newGame data.GameEntry

	err := c.BindJSON(&newGame)
	if err != nil {
		return
	}

	// If entry exists then update the details.
	for index, element := range games {
		if element.Title == newGame.Title {
			games[index].Details = newGame.Details
		}
	}

	// Write to new file
	db.CreateNew(games)
	c.IndentedJSON(http.StatusOK, newGame)
}
