package fr.lteconsulting;

import fr.lteconsulting.Mandatory;
import fr.lteconsulting.UseBuilderGenerator;

public class SuperRelou
{
	private String nom;

	private String prenom;

	private String adresse;

	private String nationalite;

	private String cursus;

	private String description;

	@UseBuilderGenerator
	public SuperRelou(@Mandatory String nom, @Mandatory String prenom, String adresse, String nationalite,
			String cursus, String description)
	{
		this.nom = nom;
		this.prenom = prenom;
		this.adresse = adresse;
		this.nationalite = nationalite;
		this.cursus = cursus;
		this.description = description;
	}

	public String getNom()
	{
		return nom;
	}

	public void setNom(String nom)
	{
		this.nom = nom;
	}

	public String getPrenom()
	{
		return prenom;
	}

	public void setPrenom(String prenom)
	{
		this.prenom = prenom;
	}

	public String getAdresse()
	{
		return adresse;
	}

	public void setAdresse(String adresse)
	{
		this.adresse = adresse;
	}

	public String getNationalite()
	{
		return nationalite;
	}

	public void setNationalite(String nationalite)
	{
		this.nationalite = nationalite;
	}

	public String getCursus()
	{
		return cursus;
	}

	public void setCursus(String cursus)
	{
		this.cursus = cursus;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}
}
