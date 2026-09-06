package api

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

type DBApi struct{}

// GetGames Returns all current entries from db.json
func (DBApi) GetGames(c *gin.Context) {
	c.IndentedJSON(http.StatusOK, "Database API GetGames")
}

// CreateNewGameEntry Creates new Game entry in the db.json
func (DBApi) CreateNewGameEntry(c *gin.Context) {
	c.IndentedJSON(http.StatusOK, "Database API CreateNewGameEntry")
}

// GetGameData Creates new Game entry in the db.json
func (DBApi) GetGameData(c *gin.Context) {
	c.IndentedJSON(http.StatusOK, "Database API GetGameData")
}

// UploadCoverImage Creates new Game Cover Image entry in the db.json
func (DBApi) UploadCoverImage(c *gin.Context) {
	c.IndentedJSON(http.StatusOK, "Database API UploadCoverImage")
}

// GetCoverImage Creates new Game Cover Image entry in the db.json
func (DBApi) GetCoverImage(c *gin.Context) {
	c.IndentedJSON(http.StatusOK, "Database API UploadCoverImage")
}

// UpdateGameEntry Update game entry in db.json
func (DBApi) UpdateGameEntry(c *gin.Context) {
	c.IndentedJSON(http.StatusOK, "Database API UpdateGameEntry")
}

// AddPlayEntry Creates new play entry in the game directory.
func (DBApi) AddPlayEntry(c *gin.Context) {
	c.IndentedJSON(http.StatusOK, "Database API AddPlayEntry")
}

// UpdatePlayEntry Updates a play entry.
func (DBApi) UpdatePlayEntry(c *gin.Context) {
	c.IndentedJSON(http.StatusOK, "Database API UpdatePlayEntry")
}
