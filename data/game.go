package data

type GameEntry struct {
	GameTitle string
	Details   VideoGameDetails
}

type VideoGameDetails struct {
	Description     string
	ReleasePlatform string
	ReleaseDate     string
	Developer       string
	MetaRating      string
	UserRating      string
	PhysicalCopy    string
	CoverImage      string
}

type PlayEntry struct {
	GameTitle string
	Details   GameEntryDetails
}

type PlayEntries struct {
	GameTitle string
	Details   []GameEntryDetails
}

type GameEntryDetails struct {
	Id                int32
	PlaythroughRating string
	CompletionDate    string
	PlatformPlayedOn  string
	TimeToBeat        string
	CompletionRate    string
	Coop              string
	Location          string
	PlayReview        Review
}

type Review struct {
	ReviewDate  string
	ReviewTitle string
	Review      string
	Rating      string
}

type Image struct {
	Location string
	Title    string
}
