package api

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

type DBApi struct{}

// Returns all current entries from db.json
func (DBApi) GetGames(c *gin.Context) {
	c.IndentedJSON(http.StatusOK, "Database API GetGames")
}

// Creates new Game entry in the db.json
func (DBApi) CreateNewGameEntry(c *gin.Context) {
	c.IndentedJSON(http.StatusOK, "Database API CreateNewGameEntry")
}

// Update game entry in db.json
func (DBApi) UpdateGameEntry(c *gin.Context) {
	c.IndentedJSON(http.StatusOK, "Database API UpdateGameEntry")
}

// Creates new play entry in the game directory.
func (DBApi) AddPlayEntry(c *gin.Context) {
	c.IndentedJSON(http.StatusOK, "Database API AddPlayEntry")
}

// Updates a play entry.
func (DBApi) UpdatePlayEntry(c *gin.Context) {
	c.IndentedJSON(http.StatusOK, "Database API UpdatePlayEntry")
}
