package ru.astakhovmd.Arena_plugin;

class NameTaken extends Exception
{
    // Parameterless Constructor
    public NameTaken() {}

    // Constructor that accepts a message
    public NameTaken(String message)
    {
        super(message);
    }
}

class NotSameWorld extends Exception
{
    // Parameterless Constructor
    public NotSameWorld() {}

    // Constructor that accepts a message
    public NotSameWorld(String message)
    {
        super(message);
    }
}
class NoRegionTypeSet extends Exception
{
    // Parameterless Constructor
    public NoRegionTypeSet() {}

    // Constructor that accepts a message
    public NoRegionTypeSet(String message)
    {
        super(message);
    }
}
