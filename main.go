package main

import (
	"gametracker/api"

	"github.com/gin-gonic/gin"
)

func main() {

	router := gin.Default()

	router.GET("/games", api.GetGames)
	router.POST("/createNew", api.CreateNew)
	router.POST("/updateGame", api.UpdateGame)
	router.Run("localhost:8080")
}
