package main

import (
	"gametracker/api"
	"gametracker/db"
	"gametracker/util"
	"github.com/gin-gonic/gin"
	"log"
)

func main() {
	config := util.Config{}

	confErr := config.ReadConfigFile()
	if confErr != nil {
		log.Fatalf("Failed to read config file")
		return
	}

	if util.GlobalConfig.ServerPort == "" {
		log.Fatalf("Please provide config file")
		return
	}

	// Create default directories.
	db.CheckAndCreateDir(util.GlobalConfig.MainJsonDbDirectory)
	db.CheckAndCreateDir(util.GlobalConfig.CoverImageDirectory)

	// Start web server
	router := gin.Default()
	// Select the default API for the backend.
	db := defaultAPI(util.GlobalConfig.DefaultDB)
	router.Use(CORSMiddleware())
	router.GET("/getAllGames", db.GetGames)
	router.GET("/getGameEntry", db.GetGameData)
	router.POST("/newGameEntry", db.CreateNewGameEntry)
	router.DELETE("/deleteGameEntry/:id", db.DeleteGameEntry)
	router.POST("/uploadCoverImage", db.UploadCoverImage)
	router.GET("/getCoverImage", db.GetCoverImage)
	router.PUT("/updateGameEntry", db.UpdateGameEntry)
	router.POST("/addPlayEntry", db.AddPlayEntry)
	router.PUT("/updatePlayEntry", db.UpdatePlayEntry)
	err := router.Run()

	//err := router.RunTLS(":"+util.GlobalConfig.ServerPort, util.GlobalConfig.PathToCertificate, util.GlobalConfig.PathToPrivatekey)
	if err != nil {
		log.Println(err)
	}
}

func defaultAPI(dbType string) api.Model {
	switch dbType {
	case "db":
		return api.DBApi{}
	default:
		return api.JSONApi{}
	}
}

// CORSMiddleware Lazy CORS fix but it works for now.
func CORSMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Writer.Header().Set("Access-Control-Allow-Origin", "*")
		c.Writer.Header().Set("Access-Control-Allow-Credentials", "true")
		c.Writer.Header().Set("Access-Control-Allow-Headers", "Content-Type, Content-Length, Accept-Encoding, X-CSRF-Token, Authorization, accept, origin, Cache-Control, X-Requested-With")
		c.Writer.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS, GET, PUT, DELETE")

		if c.Request.Method == "OPTIONS" {
			c.AbortWithStatus(204)
			return
		}

		c.Next()
	}
}
