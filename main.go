package main

import (
	"gametracker/api"
	"log"

	"github.com/gin-gonic/gin"
)

func main() {

	router := gin.Default()

	router.GET("/getAllGames", api.GetGames)
	router.POST("/newGameEntry", api.CreateNew)
	router.PUT("/updateGameEntry", api.UpdateGame)
	router.POST("/addPlayEntry", api.AddPlayEntry)

	err := router.RunTLS(":8080", "certificate.pem", "private.key")
	if err != nil {
		log.Println(err)
	}
}
