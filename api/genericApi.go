package api

import (
	"github.com/gin-gonic/gin"
)

type Model interface {
	GetGames(c *gin.Context)
	CreateNewGameEntry(c *gin.Context)
	DeleteGameEntry(c *gin.Context)
	GetGameData(c *gin.Context)
	UploadCoverImage(c *gin.Context)
	GetCoverImage(c *gin.Context)
	UpdateGameEntry(c *gin.Context)
	AddPlayEntry(c *gin.Context)
	UpdatePlayEntry(c *gin.Context)
}
